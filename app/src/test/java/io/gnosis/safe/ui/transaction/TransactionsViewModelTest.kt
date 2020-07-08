package io.gnosis.safe.ui.transaction

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.models.*
import io.gnosis.data.models.TransactionStatus.*
import io.gnosis.data.repositories.ENS_ERC721_TOKEN_INFO
import io.gnosis.data.repositories.NFT_ERC721_TOKEN_INFO
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transaction.TransactionsViewModel.Companion.OPACITY_FULL
import io.gnosis.safe.ui.transaction.TransactionsViewModel.Companion.OPACITY_HALF
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
    private val defaultToAddress: Solidity.Address = "0x12345678".asEthereumAddress()!!
    private val defaultFromAddress: Solidity.Address = "0x1234567890".asEthereumAddress()!!
    private val defaultSafe = Safe(defaultSafeAddress, defaultSafeName)
    private val defaultThreshold: Int = 2
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

        testObserver.assertValueCount(3)
        with(testObserver.values()[0]) {
            assertEquals(true, isLoading)
            assertEquals(null, viewAction)
        }
        with(testObserver.values()[1]) {
            assertEquals(true, isLoading)
            assertEquals(ActiveSafeChanged(safe), viewAction)
        }
        with(testObserver.values()[2]) {
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

        testObserver.assertValueCount(3)
        with(testObserver.values()[0]) {
            assertEquals(true, isLoading)
            assertEquals(null, viewAction)
        }
        with(testObserver.values()[1]) {
            assertEquals(true, isLoading)
            assertEquals(ActiveSafeChanged(safe), viewAction)
        }
        with(testObserver.values()[2]) {
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

    @Test
    fun `load (tx list with historic token transfers) should emit updates with token transfer`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns Page(
            1, "", "",
            listOf(
                buildTransfer(serviceTokenInfo = ENS_ERC721_TOKEN_INFO, sender = defaultFromAddress, recipient = defaultSafeAddress),
                buildTransfer(serviceTokenInfo = NFT_ERC721_TOKEN_INFO, sender = defaultFromAddress, recipient = defaultSafeAddress),
                buildTransfer(serviceTokenInfo = createErc20ServiceToken(), status = Cancelled),
                buildTransfer(serviceTokenInfo = ETH_SERVICE_TOKEN_INFO, value = BigInteger("100000000000000"), status = Failed)
            )
        )
        coEvery { safeRepository.getActiveSafe() } returns defaultSafe
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(defaultSafeAddress, defaultNonce, defaultThreshold)
        transactionsViewModel = TransactionsViewModel(transactionRepository, safeRepository, appDispatchers)

        transactionsViewModel.load()

        transactionsViewModel.state.observeForever(stateObserver)
        with(stateObserver.values()[0]) {
            assertEquals(true, viewAction is LoadTransactions)
            with((viewAction as LoadTransactions).newTransactions) {
                assertEquals(5, size)
                assertEquals(
                    TransactionView.SectionHeader(title = io.gnosis.safe.R.string.tx_list_history),
                    this[0]
                )
                assertEquals(
                    TransactionView.Transfer(
                        status = Success,
                        statusText = R.string.tx_list_success,
                        statusColorRes = R.color.safe_green,
                        amountText = "+1 ENS",
                        amountColor = R.color.safe_green,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_green_16dp,
                        address = defaultFromAddress,
                        alpha = OPACITY_FULL
                    ),
                    this[1]
                )
                assertEquals(
                    TransactionView.Transfer(
                        status = Success,
                        statusText = R.string.tx_list_success,
                        statusColorRes = R.color.safe_green,
                        amountText = "+1 NFT",
                        amountColor = R.color.safe_green,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_green_16dp,
                        address = defaultFromAddress,
                        alpha = OPACITY_FULL
                    ),
                    this[2]
                )
                assertEquals(
                    TransactionView.Transfer(
                        status = Cancelled,
                        statusText = R.string.tx_list_cancelled,
                        statusColorRes = R.color.dark_grey,
                        amountText = "-1 AQER",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_red_10dp,
                        address = defaultToAddress,
                        alpha = OPACITY_HALF
                    ),
                    this[3]
                )
                assertEquals(
                    TransactionView.Transfer(
                        status = Failed,
                        statusText = R.string.tx_list_failed,
                        statusColorRes = R.color.safe_failed_red,
                        amountText = "-0.0001 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_red_10dp,
                        address = defaultToAddress,
                        alpha = OPACITY_HALF
                    ),
                    this[4]
                )
            }
        }
        callVerification()
    }


    @Test
    fun `load (tx list with historic custom txs) should emit updates with Custom transactions`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns Page(
            1, "", "",
            listOf(
                buildCustom(value = BigInteger("100000000000000"), address = defaultSafeAddress),
                buildCustom(status = Failed),
                buildCustom(status = Cancelled, value = BigInteger("100000000000000"))
            )
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
                    TransactionView.SectionHeader(title = R.string.tx_list_history),
                    this[0]
                )
                assertEquals(
                    TransactionView.CustomTransaction(
                        status = Success,
                        statusText = R.string.tx_list_success,
                        statusColorRes = R.color.safe_green,
                        amountText = "+0.0001 ETH",
                        amountColor = R.color.safe_green,
                        dateTimeText = "",
                        address = defaultSafeAddress,
                        alpha = OPACITY_FULL,
                        dataSizeText = ""
                    ),
                    this[1]
                )
                assertEquals(
                    TransactionView.CustomTransaction(
                        status = Failed,
                        statusText = R.string.tx_list_failed,
                        statusColorRes = R.color.safe_failed_red,
                        amountText = "0 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        address = defaultToAddress,
                        alpha = OPACITY_HALF,
                        dataSizeText = ""
                    ),
                    this[2]
                )
                assertEquals(
                    TransactionView.CustomTransaction(
                        status = Cancelled,
                        statusText = R.string.tx_list_cancelled,
                        statusColorRes = R.color.dark_grey,
                        amountText = "-0.0001 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        address = defaultToAddress,
                        alpha = OPACITY_HALF,
                        dataSizeText = ""
                    ),
                    this[3]
                )
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
            Transaction.Transfer(status, 2, defaultToAddress, defaultFromAddress, BigInteger.ONE, "", ETH_SERVICE_TOKEN_INFO, defaultNonce)
        }
        return Page(1, "", "", transfers)
    }

    private fun buildTransfer(
        status: TransactionStatus = Success,
        confirmations: Int = 0,
        recipient: Solidity.Address = defaultToAddress,
        sender: Solidity.Address = defaultFromAddress,
        value: BigInteger = BigInteger.ONE,
        date: String = "",
        serviceTokenInfo: ServiceTokenInfo = ETH_SERVICE_TOKEN_INFO,
        nonce: BigInteger = defaultNonce
    ): Transaction =
        Transaction.Transfer(
            status = status,
            confirmations = confirmations,
            recipient = recipient,
            sender = sender,
            value = value,
            date = date,
            tokenInfo = serviceTokenInfo,
            nonce = nonce
        )

    private fun buildCustom(
        status: TransactionStatus = Success,
        confirmations: Int = 0,
        value: BigInteger = BigInteger.ZERO,
        date: String = "",
        nonce: BigInteger = defaultNonce,
        address: Solidity.Address = defaultToAddress,
        dataSize: Long = 0
    ): Transaction =
        Transaction.Custom(
            status = status,
            confirmations = confirmations,
            value = value,
            date = date,
            nonce = nonce,
            address = address,
            dataSize = dataSize
        )

    private fun buildSettingsChange(
        status: TransactionStatus = Success,
        confirmations: Int = 0,
        date: String = "",
        nonce: BigInteger = defaultNonce,
        dataDecoded: DataDecodedDto = DataDecodedDto("addOwner", listOf())
    ): Transaction =
        Transaction.SettingsChange(
            status = status,
            confirmations = confirmations,
            date = date,
            nonce = nonce,
            dataDecoded = dataDecoded
        )

    private fun createErc20ServiceToken() = ServiceTokenInfo(
        type = ServiceTokenInfo.TokenType.ERC20,
        address = "0x63704B63Ac04f3a173Dfe677C7e3D330c347CD88".asEthereumAddress()!!,
        name = "TEST AQER",
        symbol = "AQER",
        decimals = 0,
        logoUri = "local::ethereum"
    )

    private fun buildMockSettingsChange(): Transaction.SettingsChange =
        Transaction.SettingsChange(
            Success,
            2,
            DataDecodedDto("method", emptyList()),
            null,
            BigInteger.TEN
        )
}
