package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.*
import io.gnosis.safe.utils.OwnerCredentials
import io.gnosis.safe.utils.OwnerCredentialsRepository
import io.mockk.*
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger


class ConfirmRejectionViewModelTest {
    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val safeRepository = mockk<SafeRepository>()
    private val ownerCredentialsRepository = mockk<OwnerCredentialsRepository>()
    private val tracker = mockk<Tracker>()

    private val viewModel = ConfirmRejectionViewModel(transactionRepository, safeRepository, ownerCredentialsRepository, tracker, appDispatchers)

    private val adapter = dataMoshi.adapter(TransactionDetails::class.java)

    @Test
    fun `proposeRejection (successful) emits RejectionSubmitted`() = runBlockingTest {
        val transactionDetailsDto = adapter.readJsonFrom("tx_details_transfer.json")
        val transactionDetails = toTransactionDetails(transactionDetailsDto)
        coEvery { safeRepository.getActiveSafe() } returns Safe("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!, "safe_name")
        coEvery { safeRepository.getSafes() } returns emptyList()
        coEvery { transactionRepository.sign(any(), any()) } returns ""
        coEvery { transactionRepository.proposeTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        coEvery { ownerCredentialsRepository.retrieveCredentials() } returns OwnerCredentials(
            "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
            BigInteger.ONE
        )
        coEvery { tracker.logTransactionRejected() } just Runs
        viewModel.txDetails = transactionDetails

        viewModel.submitRejection()

        with(viewModel.state.test().values()) {
            assertEquals(RejectionSubmitted, this[0].viewAction)
        }
        coVerify(exactly = 1) { transactionRepository.proposeTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { transactionRepository.sign(BigInteger.ONE, "a64c3d38e98284acabf6c84312dd84817fe58cbf403e7556c5cbb9d57142786a") }
        coVerify(exactly = 1) { ownerCredentialsRepository.retrieveCredentials() }
        coVerify(exactly = 2) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { tracker.logTransactionRejected() }
    }

    private suspend fun toTransactionDetails(transactionDetailsDto: TransactionDetails): TransactionDetails {
        val mockGatewayApi = mockk<GatewayApi>().apply { coEvery { loadTransactionDetails(any()) } returns transactionDetailsDto }
        return TransactionRepository(mockGatewayApi).getTransactionDetails("txId")
    }
}
