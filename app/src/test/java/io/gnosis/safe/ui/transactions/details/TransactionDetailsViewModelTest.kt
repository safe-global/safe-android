package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionStatus
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.details.viewdata.toTransactionDetailsViewData
import io.mockk.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class TransactionDetailsViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val tracker = mockk<Tracker>()

    private val viewModel = TransactionDetailsViewModel(transactionRepository, safeRepository, credentialsRepository, tracker, appDispatchers)

    private val adapter = dataMoshi.adapter(TransactionDetails::class.java)

    @Test
    fun `loadDetails (transactionRepository failure) should emit error`() = runBlockingTest {
        val throwable = Throwable()
        coEvery { transactionRepository.getTransactionDetails(any()) } throws throwable

        viewModel.loadDetails("tx_details_id")

        with(viewModel.state.test().values()) {
            assertEquals(this[0].viewAction, BaseStateViewModel.ViewAction.ShowError(throwable))
        }
        coVerify(exactly = 1) { transactionRepository.getTransactionDetails("tx_details_id") }
    }

    @Test
    fun `loadDetails (successful) should emit txDetails`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        coEvery { transactionRepository.getTransactionDetails(any()) } returns transactionDetails
        coEvery { safeRepository.getSafes() } returns emptyList()
        val expectedTransactionInfoViewData = transactionDetails.toTransactionDetailsViewData(emptyList())

        viewModel.loadDetails("tx_details_id")

        with(viewModel.state.test().values()) {
            assertEquals(UpdateDetails(expectedTransactionInfoViewData), this[0].viewAction)
        }
        coVerify(exactly = 1) { transactionRepository.getTransactionDetails("tx_details_id") }
    }

    @Test
    fun `isAwaitingOwnerConfirmation (wrong status) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_EXECUTION)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus
        )

        assertEquals(false, actual)
    }

    @Test
    fun `isAwaitingOwnerConfirmation (no owner credential) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        coEvery { credentialsRepository.ownerCount() } returns 0

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus
        )

        assertEquals(false, actual)
        coVerify(exactly = 1) { credentialsRepository.ownerCount() }
    }

    @Test
    fun `isAwaitingOwnerConfirmation (owner is not signer) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val ownerCredentials = OwnerCredentials("0x1".asEthereumAddress()!!, BigInteger.ONE)
        every { credentialsRepository.hasCredentials() } returns true
        every { credentialsRepository.retrieveCredentials() } returns ownerCredentials

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus
        )

        assertEquals(false, actual)
        verify(exactly = 1) { credentialsRepository.hasCredentials() }
        verify(exactly = 1) { credentialsRepository.retrieveCredentials() }
    }

    @Test
    fun `isAwaitingOwnerConfirmation (owner is signer but has already signed) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val ownerCredentials = OwnerCredentials("0x65F8236309e5A99Ff0d129d04E486EBCE20DC7B0".asEthereumAddress()!!, BigInteger.ONE)
        every { credentialsRepository.hasCredentials() } returns true
        every { credentialsRepository.retrieveCredentials() } returns ownerCredentials

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus
        )

        assertEquals(false, actual)
        verify(exactly = 1) { credentialsRepository.hasCredentials() }
        verify(exactly = 1) { credentialsRepository.retrieveCredentials() }
    }

    @Test
    fun `isAwaitingOwnerConfirmation (successful) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val ownerCredentials = OwnerCredentials("0x8bc9Ab35a2A8b20ad8c23410C61db69F2e5d8164".asEthereumAddress()!!, BigInteger.ONE)
        every { credentialsRepository.hasCredentials() } returns true
        every { credentialsRepository.retrieveCredentials() } returns ownerCredentials

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus
        )

        assertEquals(true, actual)
        verify(exactly = 1) { credentialsRepository.hasCredentials() }
        verify(exactly = 1) { credentialsRepository.retrieveCredentials() }
    }

    @Test
    fun `submitConfirmation (invalid safeTxHash) emits error MismatchingSafeTxHash`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertEquals(this[0].viewAction, BaseStateViewModel.ViewAction.ShowError(MismatchingSafeTxHash))
        }
        coVerify(exactly = 0) { transactionRepository.submitConfirmation(any(), any()) }
    }

    @Test
    fun `submitConfirmation (no owner credentials) emits error MissingOwnerCredentials`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { credentialsRepository.retrieveCredentials() } returns null
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertEquals(this[0].viewAction, BaseStateViewModel.ViewAction.ShowError(MissingOwnerCredential))
        }
        coVerify(exactly = 0) { transactionRepository.submitConfirmation(any(), any()) }
        coVerify(exactly = 1) { credentialsRepository.retrieveCredentials() }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
    }

    @Test
    fun `submitConfirmation (transactionRepository Failure, sign) emits error`() = runBlockingTest {
        val throwable = Throwable()
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { transactionRepository.sign(any(), any()) } throws throwable
        coEvery { transactionRepository.submitConfirmation(any(), any()) } throws throwable
        coEvery { credentialsRepository.retrieveCredentials() } returns OwnerCredentials(
            "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
            BigInteger.ONE
        )
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertTrue(this[0].viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertTrue((this[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error is TxConfirmationFailed)
        }
        coVerify(exactly = 0) { transactionRepository.submitConfirmation(any(), any()) }
        coVerify(exactly = 1) { transactionRepository.sign(BigInteger.ONE, "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67") }
        coVerify(exactly = 1) { credentialsRepository.retrieveCredentials() }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
    }

    @Test
    fun `submitConfirmation (transactionRepository Failure, gateway) emits error`() = runBlockingTest {
        val throwable = Throwable()
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { transactionRepository.sign(any(), any()) } returns ""
        coEvery { transactionRepository.submitConfirmation(any(), any()) } throws throwable
        coEvery { credentialsRepository.retrieveCredentials() } returns OwnerCredentials(
            "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
            BigInteger.ONE
        )
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertTrue(this[0].viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertTrue((this[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error is TxConfirmationFailed)
        }
        coVerify(exactly = 1) { transactionRepository.submitConfirmation(any(), any()) }
        coVerify(exactly = 1) { transactionRepository.sign(BigInteger.ONE, "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67") }
        coVerify(exactly = 1) { credentialsRepository.retrieveCredentials() }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
    }

    @Test
    fun `submitConfirmation (successful) emits ConfirmationSubmitted`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { safeRepository.getSafes() } returns emptyList()
        coEvery { transactionRepository.sign(any(), any()) } returns ""
        coEvery { transactionRepository.submitConfirmation(any(), any()) } returns transactionDetails
        coEvery { credentialsRepository.retrieveCredentials() } returns OwnerCredentials(
            "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
            BigInteger.ONE
        )
        coEvery { tracker.logTransactionConfirmed() } just Runs
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertEquals(ConfirmationSubmitted(transactionDetails.toTransactionDetailsViewData(emptyList())), this[0].viewAction)
        }
        coVerify(exactly = 1) { transactionRepository.submitConfirmation(any(), any()) }
        coVerify(exactly = 1) { transactionRepository.sign(BigInteger.ONE, "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67") }
        coVerify(exactly = 1) { credentialsRepository.retrieveCredentials() }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { tracker.logTransactionConfirmed() }
    }

    private suspend fun toTransactionDetails(transactionDetailsDto: TransactionDetails): TransactionDetails {
        val mockGatewayApi = mockk<GatewayApi>().apply { coEvery { loadTransactionDetails(any()) } returns transactionDetailsDto }
        return TransactionRepository(mockGatewayApi).getTransactionDetails("txId")
    }
}
