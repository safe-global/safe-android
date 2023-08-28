package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionInfo
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.SemVer
import io.gnosis.data.utils.calculateSafeTxHash
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.Tracker
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.readJsonFrom
import io.gnosis.safe.test
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import java.math.BigInteger


class ConfirmRejectionViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val settingsHandler = mockk<SettingsHandler>()
    private val tracker = mockk<Tracker>()

    private lateinit var viewModel: ConfirmRejectionViewModel

    private val adapter = dataMoshi.adapter(TransactionDetails::class.java)

    @Test
    fun `submitRejection (successful) emits RejectionSubmitted`() = runTest(UnconfinedTestDispatcher()) {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val owner = Owner(
            "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
            null,
            Owner.Type.IMPORTED,
            null
        )
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { safeRepository.getSafes() } returns emptyList()
        coEvery { credentialsRepository.signWithOwner(any(), any()) } returns ECDSASignature(BigInteger.ZERO, BigInteger.ZERO)
        coEvery {
            transactionRepository.proposeTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } just Runs
        coEvery { credentialsRepository.owner(owner.address) } returns owner
        coEvery { tracker.logTransactionRejected(any()) } just Runs

        viewModel = ConfirmRejectionViewModel(transactionRepository, safeRepository, credentialsRepository, settingsHandler, tracker, appDispatchers)

        viewModel.txDetails = transactionDetails

        viewModel.submitRejection(owner.address)

        with(viewModel.state.test().values()) {
            assertEquals(RejectionSubmitted, this[0].viewAction)
        }
        coVerify(exactly = 1) {
            transactionRepository.proposeTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
        coVerify(exactly = 1) {
            credentialsRepository.signWithOwner(
                owner,
                "a64c3d38e98284acabf6c84312dd84817fe58cbf403e7556c5cbb9d57142786a".hexToByteArray()
            )
        }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { tracker.logTransactionRejected(any()) }
    }

    @Test
    fun `selectSigningOwner () `() = runTest(UnconfinedTestDispatcher()) {
        val safeAddress = "0x938bae50a210b80EA233112800Cd5Bc2e7644300".asEthereumAddress()!!
        coEvery { safeRepository.getActiveSafe() } returns Safe(safeAddress, "safe_name")
        val testObserver = TestLiveDataObserver<ConfirmationRejectedViewState>()
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val executionInfo = transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails

        viewModel = ConfirmRejectionViewModel(transactionRepository, safeRepository, credentialsRepository, settingsHandler, tracker, appDispatchers)

        viewModel.txDetails = transactionDetails
        viewModel.state.observeForever(testObserver)

        val rejectionExecutionInfo = DetailedExecutionInfo.MultisigExecutionDetails(nonce = executionInfo.nonce)
        val rejectionTxDetails = TransactionDetails(
            txInfo = TransactionInfo.Custom(to = AddressInfo(safeAddress)),
            detailedExecutionInfo = rejectionExecutionInfo,
            safeAppInfo = null
        )
        val rejectionTxHash =
            calculateSafeTxHash(
                implementationVersion = SemVer(1, 1, 0),
                chainId = Chain.ID_GOERLI,
                safeAddress = safeAddress,
                transaction = rejectionTxDetails,
                executionInfo = rejectionExecutionInfo
            ).toHexString()

        viewModel.selectSigningOwner()

        testObserver.assertValueCount(3)
        with(testObserver.values()[1]) {
            assertEquals(
                BaseStateViewModel.ViewAction.NavigateTo(
                    ConfirmRejectionFragmentDirections.actionConfirmRejectionFragmentToSigningOwnerSelectionFragment(
                        missingSigners = listOf(
                            "0x8bc9ab35a2a8b20ad8c23410c61db69f2e5d8164",
                            "0xbea2f9227230976d2813a2f8b922c22be1de1b23"
                        ).toTypedArray(),
                        signingMode = SigningMode.REJECTION,
                        chain = Chain.DEFAULT_CHAIN,
                        safeTxHash = rejectionTxHash
                    )
                ).toString(), viewAction.toString()
            )
        }
    }

    private suspend fun toTransactionDetails(transactionDetailsDto: TransactionDetails): TransactionDetails {
        val mockGatewayApi = mockk<GatewayApi>().apply { coEvery { loadTransactionDetails(transactionId = any(), chainId = any()) } returns transactionDetailsDto }
        return TransactionRepository(mockGatewayApi).getTransactionDetails(Chain.ID_MAINNET, "txId")
    }
}
