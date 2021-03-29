package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionStatus
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.transactions.details.viewdata.toTransactionDetailsViewData
import io.mockk.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import java.math.BigInteger

class TransactionDetailsViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val safeRepository = mockk<SafeRepository>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val settingsHandler = mockk<SettingsHandler>()
    private val tracker = mockk<Tracker>()

    private val viewModel = TransactionDetailsViewModel(transactionRepository, safeRepository, credentialsRepository, settingsHandler, tracker, appDispatchers)

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
        coEvery { credentialsRepository.owners() } returns listOf()
        val expectedTransactionInfoViewData = transactionDetails.toTransactionDetailsViewData(emptyList())

        viewModel.loadDetails("tx_details_id")

        with(viewModel.state.test().values()) {
            assertEquals(UpdateDetails(expectedTransactionInfoViewData, false, false, false), this[0].viewAction)
        }
        coVerify(exactly = 1) { transactionRepository.getTransactionDetails("tx_details_id") }
    }

    @Test
    fun `isAwaitingOwnerConfirmation (wrong status) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_EXECUTION)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus,
            listOf()
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
            transactionDetails.txStatus,
            listOf()
        )

        assertEquals(false, actual)
    }

    @Test
    fun `isAwaitingOwnerConfirmation (owner is not signer) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val owners = listOf(Owner("0x1".asEthereumAddress()!!, null, Owner.Type.LOCALLY_STORED, null))
        coEvery { credentialsRepository.ownerCount() } returns 1
        coEvery { credentialsRepository.owners() } returns owners

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus,
            owners
        )

        assertEquals(false, actual)
    }

    @Test
    fun `isAwaitingOwnerConfirmation (owner is signer but has already signed) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val owners = listOf(Owner("0x65F8236309e5A99Ff0d129d04E486EBCE20DC7B0".asEthereumAddress()!!, null, Owner.Type.LOCALLY_STORED, null))
        coEvery { credentialsRepository.ownerCount() } returns 1
        coEvery { credentialsRepository.owners() } returns owners

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus,
            owners
        )

        assertEquals(false, actual)
    }

    @Test
    fun `isAwaitingOwnerConfirmation (successful) should return false`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json").copy(txStatus = TransactionStatus.AWAITING_CONFIRMATIONS)
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val owners = listOf(Owner("0x8bc9Ab35a2A8b20ad8c23410C61db69F2e5d8164".asEthereumAddress()!!, null, Owner.Type.LOCALLY_STORED, null))
        coEvery { credentialsRepository.ownerCount() } returns 1
        coEvery { credentialsRepository.owners() } returns owners

        val actual = viewModel.isAwaitingOwnerConfirmation(
            transactionDetails.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails,
            transactionDetails.txStatus,
            owners
        )

        assertEquals(true, actual)
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
        coEvery { credentialsRepository.ownerCount() } returns 0
        coEvery { credentialsRepository.owners() } returns listOf()
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertEquals(this[0].viewAction, BaseStateViewModel.ViewAction.ShowError(MissingOwnerCredential))
        }
        coVerify(exactly = 0) { transactionRepository.submitConfirmation(any(), any()) }
        coVerify(exactly = 1) { credentialsRepository.ownerCount() }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
    }

    @Test
    fun `submitConfirmation (transactionRepository Failure, sign) emits error`() = runBlockingTest {
        val throwable = Throwable()
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val owner =  Owner("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, null, Owner.Type.LOCALLY_STORED, null)
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { credentialsRepository.signWithOwner(any(), any()) } throws throwable
        coEvery { transactionRepository.submitConfirmation(any(), any()) } throws throwable
        coEvery { credentialsRepository.ownerCount() } returns 1
        coEvery { credentialsRepository.owners() } returns listOf(owner)
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertTrue(this[0].viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertTrue((this[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error is TxConfirmationFailed)
        }
        coVerify(exactly = 0) { transactionRepository.submitConfirmation(any(), any()) }
        coVerify(exactly = 1) { credentialsRepository.signWithOwner(owner, "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67".hexToByteArray()) }
        coVerify(exactly = 1) { credentialsRepository.owners() }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
    }

    @Test
    fun `submitConfirmation (transactionRepository Failure, gateway) emits error`() = runBlockingTest {
        val throwable = Throwable()
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val owner = Owner("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, null, Owner.Type.LOCALLY_STORED, null)
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { credentialsRepository.signWithOwner(any(), any()) } returns ""
        coEvery { transactionRepository.submitConfirmation(any(), any()) } throws throwable
        coEvery { credentialsRepository.ownerCount() } returns 1
        coEvery { credentialsRepository.owners() } returns listOf(owner)
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertTrue(this[0].viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertTrue((this[0].viewAction as BaseStateViewModel.ViewAction.ShowError).error is TxConfirmationFailed)
        }
        coVerify(exactly = 1) { transactionRepository.submitConfirmation(any(), any()) }
        coVerify(exactly = 1) { credentialsRepository.signWithOwner(owner, "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67".hexToByteArray()) }
        coVerify(exactly = 1) { credentialsRepository.owners() }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
    }

    @Test
    fun `submitConfirmation (successful) emits ConfirmationSubmitted`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        val owner = Owner("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, null, Owner.Type.LOCALLY_STORED, null)
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { safeRepository.getSafes() } returns emptyList()
        coEvery { credentialsRepository.signWithOwner(any(), any()) } returns ""
        coEvery { transactionRepository.submitConfirmation(any(), any()) } returns transactionDetails
        coEvery { credentialsRepository.ownerCount() } returns 1
        coEvery { credentialsRepository.owners() } returns listOf(owner)
        coEvery { tracker.logTransactionConfirmed() } just Runs
        viewModel.txDetails = transactionDetails

        viewModel.submitConfirmation(transactionDetails)

        with(viewModel.state.test().values()) {
            assertEquals(ConfirmationSubmitted(transactionDetails.toTransactionDetailsViewData(emptyList()), false, false, false), this[0].viewAction)
        }
        coVerify(exactly = 1) { transactionRepository.submitConfirmation(any(), any()) }
        coVerify(exactly = 1) { credentialsRepository.signWithOwner(owner, "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67".hexToByteArray()) }
        coVerify(exactly = 1) { credentialsRepository.owners() }
        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { tracker.logTransactionConfirmed() }
    }

    private suspend fun toTransactionDetails(transactionDetailsDto: TransactionDetails): TransactionDetails {
        val mockGatewayApi = mockk<GatewayApi>().apply { coEvery { loadTransactionDetails(any()) } returns transactionDetailsDto }
        return TransactionRepository(mockGatewayApi).getTransactionDetails("txId")
    }
}
