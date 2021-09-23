package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.mockk.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray


class ConfirmRejectionViewModelTest {
    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val settingsHandler = mockk<SettingsHandler>()
    private val tracker = mockk<Tracker>()

    private val viewModel =
        ConfirmRejectionViewModel(transactionRepository, safeRepository, credentialsRepository, settingsHandler, tracker, appDispatchers)

    private val adapter = dataMoshi.adapter(TransactionDetails::class.java)

    @Test
    fun `submitRejection (successful) emits RejectionSubmitted`() = runBlockingTest {
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
        coEvery { credentialsRepository.signWithOwner(any(), any()) } returns ""
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
                any()
            )
        } just Runs
        coEvery { credentialsRepository.owner(owner.address) } returns owner
        coEvery { tracker.logTransactionRejected(any()) } just Runs
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
    fun `selectSigningOwner () `() = runBlockingTest {
        val testObserver = TestLiveDataObserver<ConfirmationRejectedViewState>()
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        viewModel.txDetails = transactionDetails
        viewModel.state.observeForever(testObserver)

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
                        isConfirmation = false,
                        safeTxHash = "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67"
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
