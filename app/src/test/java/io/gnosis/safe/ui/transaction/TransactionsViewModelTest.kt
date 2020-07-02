package io.gnosis.safe.ui.transaction

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.models.*
import io.gnosis.data.models.TransactionStatus.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class TransactionsViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private lateinit var transactionsViewModel: TransactionsViewModel

    private val safeRepository = mockk<SafeRepository>()
    private val transactionRepository = mockk<TransactionRepository>()

    private val defaultSafeName: String = "Default Name"
    private val defaultSafeAddress: Solidity.Address = "0x1234".asEthereumAddress()!!
    private val defaultAddress: Solidity.Address = Solidity.Address(BigInteger.ZERO)
    private val defaultSafe = Safe(defaultSafeAddress, defaultSafeName)
    private val defaultThreshold: Int = 23
    private val defaultNonce: BigInteger = BigInteger.ONE
    private val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()

    @Test
    fun `init - (no active safe change) should emit Loading`() {
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(true, isLoading)
            assertEquals(null, viewAction)
        }
        coVerify(exactly = 1) { safeRepository.activeSafeFlow() }
        coVerify { transactionRepository wasNot Called }
    }

    @Test
    fun `init - (active safe available, no transactions) should emit ShowEmptyState`() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val safeInfo = SafeInfo(safe.address, BigInteger.TEN, 2)
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        coEvery { safeRepository.activeSafeFlow() } returns flow { emit(safe) }
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { transactionRepository.getTransactions(any(), any()) } returns Page(0, null, null, emptyList())
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(false, isLoading)
            assertEquals(BaseStateViewModel.ViewAction.ShowEmptyState, viewAction)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
            transactionRepository.getTransactions(safe.address, safeInfo)
        }
    }

    @Test
    fun `init - (safeRepository failure) should emit ShowError`() {
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        val throwable = Throwable()
        coEvery { safeRepository.activeSafeFlow() } throws throwable
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(true, viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertEquals(throwable, (viewAction as BaseStateViewModel.ViewAction.ShowError).error)
        }
        coVerify(exactly = 1) { safeRepository.activeSafeFlow() }
        coVerify { transactionRepository wasNot Called }
    }

    @Test
    fun `load - (active safe no transactions) should emit ShowEmptyState`() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val safeInfo = SafeInfo(safe.address, BigInteger.TEN, 2)
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { transactionRepository.getTransactions(any(), any()) } returns Page(0, null, null, emptyList())
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)
        transactionsViewModel.load()

        testObserver.assertValueCount(2)
        with(testObserver.values()[0]) {
            assertEquals(true, isLoading)
            assertEquals(null, viewAction)
        }
        with(testObserver.values()[1]) {
            assertEquals(false, isLoading)
            assertEquals(BaseStateViewModel.ViewAction.ShowEmptyState, viewAction)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
            transactionRepository.getTransactions(safe.address, safeInfo)
        }
    }

    @Test
    fun `load - (active safe with transactions) should emit LoadTransaction`() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val safeInfo = SafeInfo(safe.address, BigInteger.TEN, 2)
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        val results = listOf(buildMockSettingsChange())
        coEvery { transactionRepository.getTransactions(any(), any()) } returns Page(0, null, null, results)
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)
        transactionsViewModel.load()

        testObserver.assertValueCount(2)
        with(testObserver.values()[0]) {
            assertEquals(true, isLoading)
            assertEquals(null, viewAction)
        }
        with(testObserver.values()[1]) {
            assertEquals(false, isLoading)
            assertEquals(true, viewAction is LoadTransactions)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
            transactionRepository.getTransactions(safe.address, safeInfo)
        }
    }

    @Test
    fun `load - (transactionRepository failure) should emit ShowError`() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val safeInfo = SafeInfo(safe.address, BigInteger.TEN, 2)
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        val throwable = Throwable()
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { transactionRepository.getTransactions(any(), any()) } throws throwable
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)
        transactionsViewModel.load()

        with(testObserver.values()[1]) {
            assertEquals(true, viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertEquals(throwable, (viewAction as BaseStateViewModel.ViewAction.ShowError).error)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
            transactionRepository.getTransactions(safe.address, safeInfo)
        }
    }

    @Test
    fun `load (tx list with queued and historic transfer) should emit updates with queued and historic sections`() {
        coEvery { safeRepository.getActiveSafe() } returns defaultSafe
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(defaultSafeAddress, defaultNonce, defaultThreshold)
        coEvery { transactionRepository.getTransactions(any(), any()) } returns createTransactionListWithStatus(Pending, Success)
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.load()

        transactionsViewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertEquals(true, viewAction is LoadTransactions)
            with((viewAction as LoadTransactions).newTransactions) {
                assertEquals(4, size)
                assertEquals(
                    TransactionView.SectionHeader(title = io.gnosis.safe.R.string.tx_list_queue),
                    this[0]
                )
                assertEquals(true, this[1] is TransactionView.TransferQueued)
                assertEquals(
                    TransactionView.SectionHeader(title = io.gnosis.safe.R.string.tx_list_history),
                    this[2]
                )
                assertEquals(true, this[3] is TransactionView.Transfer)
            }
        }
        callVerification()
    }

    @Test
    fun `load (tx list with no transfer) should emit updates with no sections`() {
        coEvery { safeRepository.getActiveSafe() } returns defaultSafe
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(defaultSafeAddress, defaultNonce, defaultThreshold)
        coEvery { transactionRepository.getTransactions(any(), any()) } returns createTransactionListWithStatus()
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.load()

        transactionsViewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertEquals(true, viewAction is BaseStateViewModel.ViewAction.ShowEmptyState)
        }
        callVerification()
    }

    @Test
    fun `load (tx list with queued transfers) should emit updates with queued sections`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns createTransactionListWithStatus(
            Pending,
            AwaitingExecution,
            AwaitingConfirmations
        )
        coEvery { safeRepository.getActiveSafe() } returns defaultSafe
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(defaultSafeAddress, defaultNonce, defaultThreshold)
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)


        transactionsViewModel.load()

        transactionsViewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertEquals(true, viewAction is LoadTransactions)
            with((viewAction as LoadTransactions).newTransactions) {
                assertEquals(4, size)
                assertEquals(
                    TransactionView.SectionHeader(title = io.gnosis.safe.R.string.tx_list_queue),
                    this[0]
                )
                assertEquals(true, this[1] is TransactionView.TransferQueued)
                assertEquals(true, this[2] is TransactionView.TransferQueued)
                assertEquals(true, this[3] is TransactionView.TransferQueued)
            }
        }
        callVerification()
    }

    @Test
    fun `load (tx list with historic transfers) should emit updates with historic sections`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns createTransactionListWithStatus(Success, Failed, Cancelled)
        coEvery { safeRepository.getActiveSafe() } returns defaultSafe
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(defaultSafeAddress, defaultNonce, defaultThreshold)
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.load()

        transactionsViewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertEquals(true, viewAction is LoadTransactions)
            with((viewAction as LoadTransactions).newTransactions) {
                assertEquals(4, size)
                assertEquals(
                    TransactionView.SectionHeader(title = io.gnosis.safe.R.string.tx_list_history),
                    this[0]
                )
                assertEquals(true, this[1] is TransactionView.Transfer)
                assertEquals(true, this[2] is TransactionView.Transfer)
                assertEquals(true, this[3] is TransactionView.Transfer)
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
            Transaction.Transfer(status, 2, defaultAddress, defaultAddress, BigInteger.ONE, "", ETH_SERVICE_TOKEN_INFO, defaultNonce)
        }
        return Page(1, "", "", transfers)
    }

    private fun buildMockSettingsChange(): Transaction.SettingsChange =
        Transaction.SettingsChange(
            Success,
            2,
            DataDecodedDto("method", emptyList()),
            null,
            BigInteger.TEN
        )
}
