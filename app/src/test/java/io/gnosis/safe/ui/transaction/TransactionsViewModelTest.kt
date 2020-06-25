package io.gnosis.safe.ui.transaction

import io.gnosis.data.models.*
import io.gnosis.data.models.TransactionStatus.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.R
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class TransactionsViewModelTest {
    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private lateinit var viewModel: TransactionsViewModel
    private val defaultSafeName: String = "Default Name"
    private val defaultSafeAddress: Solidity.Address = "0x1234".asEthereumAddress()!!
    private val defaultAddress: Solidity.Address = Solidity.Address(BigInteger.ZERO)
    private val defaultSafe = Safe(defaultSafeAddress, defaultSafeName)
    private val defaultThreshold: Int = 23
    private val defaultNonce: BigInteger = BigInteger.ONE
    private val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

    @Before
    fun setup() {
        viewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)
        Dispatchers.setMain(TestCoroutineDispatcher())

        coEvery { safeRepository.getActiveSafe() } returns defaultSafe
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(defaultSafeAddress, defaultNonce, defaultThreshold)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load (tx list with queued and historic transfer) should emit updates with queued and historic sections`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns createTransactionListWithStatus(Pending, Success)

        viewModel.load()

        viewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertTrue(viewAction is LoadTransactions)
            with((viewAction as LoadTransactions).newTransactions) {
                assertEquals(4, size)
                assertEquals(TransactionView.SectionHeader(title = R.string.tx_list_queue), this[0])
                assertTrue(this[1] is TransactionView.Transfer)
                assertEquals(TransactionView.SectionHeader(title = R.string.tx_list_history), this[2])
                assertTrue(this[3] is TransactionView.Transfer)
            }
        }
        callVerification()
    }

    @Test
    fun `load (tx list with no transfer) should emit updates with no sections`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns createTransactionListWithStatus()

        viewModel.load()

        viewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertTrue(viewAction is LoadTransactions)
            with((viewAction as LoadTransactions).newTransactions) {
                assertEquals(0, size)
            }
        }
        callVerification()
    }

    @Test
    fun `load (tx list with queued transfers) should emit updates with queued sections`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns createTransactionListWithStatus(Pending, AwaitingExecution, AwaitingConfirmation)

        viewModel.load()

        viewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertTrue(viewAction is LoadTransactions)
            with((viewAction as LoadTransactions).newTransactions) {
                assertEquals(4, size)
                assertEquals(TransactionView.SectionHeader(title = R.string.tx_list_queue), this[0])
                assertTrue(this[1] is TransactionView.Transfer)
                assertTrue(this[2] is TransactionView.Transfer)
                assertTrue(this[3] is TransactionView.Transfer)
            }
        }
        callVerification()
    }

    @Test
    fun `load (tx list with historic transfers) should emit updates with historic sections`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns createTransactionListWithStatus(Success, Failed, Cancelled)

        viewModel.load()

        viewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertTrue(viewAction is LoadTransactions)
            with((viewAction as LoadTransactions).newTransactions) {
                assertEquals(4, size)
                assertEquals(TransactionView.SectionHeader(title = R.string.tx_list_history), this[0])
                assertTrue(this[1] is TransactionView.Transfer)
                assertTrue(this[2] is TransactionView.Transfer)
                assertTrue(this[3] is TransactionView.Transfer)
            }
        }
        callVerification()
    }

    private fun callVerification() {
        coVerify { safeRepository.getActiveSafe() }
        coVerify { safeRepository.getSafeInfo(defaultSafeAddress) }
        coVerify { transactionRepository.getTransactions(defaultSafeAddress, SafeInfo(defaultSafeAddress, defaultNonce, defaultThreshold)) }
    }

    private fun createTransactionListWithStatus(vararg transactionStatus: TransactionStatus): Page<Transaction> {
        val transfers = transactionStatus.map { status ->
            Transaction.Transfer(status, defaultAddress, defaultAddress, BigInteger.ONE, "", ETH_SERVICE_TOKEN_INFO)
        }
        return Page(1, "", "", transfers)
    }
}
