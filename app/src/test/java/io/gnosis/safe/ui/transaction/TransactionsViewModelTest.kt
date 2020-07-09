package io.gnosis.safe.ui.transaction

import android.view.View
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.models.*
import io.gnosis.data.models.TransactionStatus.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_MASTER_COPY
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_DISABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ENABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_REMOVE_OWNER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SET_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_1_0_0
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_1_1_1
import io.gnosis.data.repositories.TokenRepository.Companion.ERC20_FALLBACK_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.TokenRepository.Companion.ERC721_FALLBACK_SERVICE_TOKEN_INFO
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
import pm.gnosis.utils.asEthereumAddressString
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
    private val defaultModuleAddress: Solidity.Address = "0x25F73b24B866963B0e560fFF9bbA7908be0263E8".asEthereumAddress()!!
    private val defaultFallbackHandler: Solidity.Address = "0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44".asEthereumAddress()!!
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
                    TransactionView.SectionHeader(title = R.string.tx_list_queue),
                    this[0]
                )
                assertEquals(true, this[1] is TransactionView.TransferQueued)
                assertEquals(
                    TransactionView.SectionHeader(title = R.string.tx_list_history),
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
                    TransactionView.SectionHeader(title = R.string.tx_list_queue),
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
                    TransactionView.SectionHeader(title = R.string.tx_list_history),
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
                buildTransfer(serviceTokenInfo = ERC20_FALLBACK_SERVICE_TOKEN_INFO, sender = defaultFromAddress, recipient = defaultSafeAddress),
                buildTransfer(serviceTokenInfo = ERC721_FALLBACK_SERVICE_TOKEN_INFO, sender = defaultFromAddress, recipient = defaultSafeAddress),
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
                    TransactionView.SectionHeader(title = R.string.tx_list_history),
                    this[0]
                )
                assertEquals(
                    TransactionView.Transfer(
                        status = Success,
                        statusText = R.string.tx_list_success,
                        statusColorRes = R.color.safe_green,
                        amountText = "+1 ERC20",
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
    fun `load (tx list with queued and historic ether transfers) should emit updates with ether transfer`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns Page(
            1, "", "",
            listOf(
                buildTransfer(
                    status = AwaitingConfirmations,
                    confirmations = 0,
                    serviceTokenInfo = ETH_SERVICE_TOKEN_INFO,
                    value = BigInteger.ZERO,
                    recipient = defaultToAddress // outgoing
                ),
                buildTransfer(
                    status = AwaitingExecution,
                    confirmations = 2,
                    serviceTokenInfo = ETH_SERVICE_TOKEN_INFO,
                    value = BigInteger.ZERO,
                    recipient = defaultSafeAddress // incoming
                ),
                buildTransfer(serviceTokenInfo = ETH_SERVICE_TOKEN_INFO, value = BigInteger("100000000000000"), status = Failed),
                buildTransfer(serviceTokenInfo = ETH_SERVICE_TOKEN_INFO, value = BigInteger.ZERO, recipient = defaultToAddress),
                buildTransfer(serviceTokenInfo = ETH_SERVICE_TOKEN_INFO, value = BigInteger.ZERO, recipient = defaultSafeAddress),
                buildTransfer(
                    serviceTokenInfo = ERC20_FALLBACK_SERVICE_TOKEN_INFO,
                    value = BigInteger.TEN,
                    recipient = defaultSafeAddress
                )
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
                assertEquals(8, size)
                assertEquals(
                    TransactionView.SectionHeader(title = R.string.tx_list_queue),
                    this[0]
                )
                assertEquals(
                    TransactionView.TransferQueued(
                        status = AwaitingConfirmations,
                        statusText = R.string.tx_list_awaiting_confirmations,
                        statusColorRes = R.color.safe_pending_orange,
                        amountText = "0 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_red_10dp,
                        address = defaultToAddress,
                        confirmations = 0,
                        nonce = "1",
                        confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                        confirmationsTextColor = R.color.medium_grey,
                        threshold = 2
                    ),
                    this[1]
                )
                assertEquals(
                    TransactionView.TransferQueued(
                        status = AwaitingExecution,
                        statusText = R.string.tx_list_awaiting_execution,
                        statusColorRes = R.color.safe_pending_orange,
                        amountText = "0 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_green_16dp,
                        address = defaultFromAddress,
                        threshold = 2,
                        confirmationsTextColor = R.color.safe_green,
                        confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                        nonce = "1",
                        confirmations = 2
                    ),
                    this[2]
                )
                assertEquals(
                    TransactionView.SectionHeader(title = R.string.tx_list_history),
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
                assertEquals(
                    TransactionView.Transfer(
                        status = Success,
                        statusText = R.string.tx_list_success,
                        statusColorRes = R.color.safe_green,
                        amountText = "0 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_red_10dp,
                        address = defaultToAddress,
                        alpha = OPACITY_FULL
                    ),
                    this[5]
                )
                assertEquals(
                    TransactionView.Transfer(
                        status = Success,
                        statusText = R.string.tx_list_success,
                        statusColorRes = R.color.safe_green,
                        amountText = "0 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_green_16dp,
                        address = defaultFromAddress,
                        alpha = OPACITY_FULL
                    ),
                    this[6]
                )
                assertEquals(
                    TransactionView.Transfer(
                        status = Success,
                        statusText = R.string.tx_list_success,
                        statusColorRes = R.color.safe_green,
                        amountText = "+10 ERC20",
                        amountColor = R.color.safe_green,
                        dateTimeText = "",
                        txTypeIcon = R.drawable.ic_arrow_green_16dp,
                        address = defaultFromAddress,
                        alpha = OPACITY_FULL
                    ),
                    this[7]
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
                buildCustom(status = AwaitingExecution, confirmations = 2),
                buildCustom(status = AwaitingConfirmations, confirmations = null),
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
                assertEquals(7, size)
                assertEquals(
                    TransactionView.SectionHeader(title = R.string.tx_list_queue),
                    this[0]
                )
                assertEquals(
                    TransactionView.CustomTransactionQueued(
                        status = AwaitingExecution,
                        statusText = R.string.tx_list_awaiting_execution,
                        statusColorRes = R.color.safe_pending_orange,
                        amountText = "0 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        address = defaultToAddress,
                        dataSizeText = "",
                        nonce = "1",
                        confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                        confirmationsTextColor = R.color.safe_green,
                        threshold = 2,
                        confirmations = 2
                    ),
                    this[1]
                )
                assertEquals(
                    TransactionView.CustomTransactionQueued(
                        status = AwaitingConfirmations,
                        statusText = R.string.tx_list_awaiting_confirmations,
                        statusColorRes = R.color.safe_pending_orange,
                        amountText = "0 ETH",
                        amountColor = R.color.gnosis_dark_blue,
                        dateTimeText = "",
                        address = defaultToAddress,
                        dataSizeText = "",
                        threshold = 2,
                        confirmationsTextColor = R.color.medium_grey,
                        confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                        nonce = "1",
                        confirmations = 0
                    ),
                    this[2]
                )
                assertEquals(
                    TransactionView.SectionHeader(title = R.string.tx_list_history),
                    this[3]
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
                    this[4]
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
                    this[5]
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
                    this[6]
                )
            }
        }
        callVerification()
    }


    @Test
    fun `load (tx list with historic setting changes) should emit updates with settings changes`() {
        coEvery { transactionRepository.getTransactions(any(), any()) } returns Page(
            1, "", "",
            listOf(
                // queued
                buildSettingsChange(
                    status = AwaitingExecution,
                    confirmations = 2,
                    dataDecoded = buildDataDecodedDto(
                        METHOD_CHANGE_MASTER_COPY,
                        listOf(ParamsDto("_masterCopy", "address", SAFE_MASTER_COPY_1_1_1.asEthereumAddressString()))
                    )
                ),
                buildSettingsChange(
                    status = AwaitingConfirmations,
                    dataDecoded = buildDataDecodedDto(METHOD_REMOVE_OWNER, listOf())
                ),
                buildSettingsChange(
                    status = AwaitingConfirmations,
                    dataDecoded = buildDataDecodedDto(METHOD_SET_FALLBACK_HANDLER, listOf())
                ),
                buildSettingsChange(
                    status = AwaitingConfirmations,
                    dataDecoded = buildDataDecodedDto(
                        METHOD_DISABLE_MODULE,
                        listOf(ParamsDto("module", "address", defaultModuleAddress.asEthereumAddressString()))
                    )
                ),
                buildSettingsChange(
                    status = AwaitingExecution,
                    confirmations = 2,
                    dataDecoded = buildDataDecodedDto(
                        METHOD_ENABLE_MODULE,
                        listOf(ParamsDto("module", "address", defaultModuleAddress.asEthereumAddressString()))
                    )
                ),
                // history
                buildSettingsChange(
                    status = Cancelled,
                    dataDecoded = buildDataDecodedDto(
                        METHOD_SET_FALLBACK_HANDLER,
                        listOf(ParamsDto("handler", "address", defaultFallbackHandler.asEthereumAddressString()))
                    )
                ),
                buildSettingsChange(
                    status = Success,
                    confirmations = 2,
                    dataDecoded = buildDataDecodedDto(
                        METHOD_CHANGE_MASTER_COPY,
                        listOf(ParamsDto("_masterCopy", "address", SAFE_MASTER_COPY_1_0_0.asEthereumAddressString()))
                    )
                ),
                buildSettingsChange(
                    status = Failed,
                    dataDecoded = buildDataDecodedDto(
                        METHOD_ENABLE_MODULE,
                        listOf(ParamsDto("module", "address", defaultModuleAddress.asEthereumAddressString()))
                    )
                )
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
                assertEquals(10, size)
                assertEquals(
                    TransactionView.SectionHeader(title = R.string.tx_list_queue),
                    this[0]
                )
                assertEquals(
                    TransactionView.SettingsChangeVariantQueued(
                        label = R.string.tx_list_change_mastercopy,
                        status = AwaitingExecution,
                        statusText = R.string.tx_list_awaiting_execution,
                        statusColorRes = R.color.safe_pending_orange,
                        dateTimeText = "",
                        address = SAFE_MASTER_COPY_1_1_1,
                        version = "1.1.1",
                        visibilityEllipsizedAddress = View.VISIBLE,
                        visibilityModuleAddress = View.GONE,
                        visibilityVersion = View.VISIBLE,
                        nonce = "1",
                        confirmations = 2,
                        confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                        confirmationsTextColor = R.color.safe_green,
                        threshold = 2
                    ),
                    this[1]
                )
                assertEquals(
                    TransactionView.SettingsChangeQueued(
                        status = AwaitingConfirmations,
                        statusText = R.string.tx_list_awaiting_confirmations,
                        statusColorRes = R.color.safe_pending_orange,
                        dateTimeText = "",
                        confirmations = 0,
                        threshold = 2,
                        confirmationsTextColor = R.color.medium_grey,
                        confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                        nonce = "1",
                        settingNameText = "removeOwner"
                    ),
                    this[2]
                )
                assertEquals(
                    TransactionView.SettingsChangeVariantQueued(
                        label = R.string.tx_list_set_fallback_handler,
                        status = AwaitingConfirmations,
                        statusText = R.string.tx_list_awaiting_confirmations,
                        statusColorRes = R.color.safe_pending_orange,
                        dateTimeText = "",
                        confirmations = 0,
                        threshold = 2,
                        confirmationsTextColor = R.color.medium_grey,
                        confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                        nonce = "1",
                        version = "DefaultFallbackHandler",
                        address = null,
                        visibilityVersion = View.VISIBLE,
                        visibilityModuleAddress = View.GONE,
                        visibilityEllipsizedAddress = View.VISIBLE
                    ),
                    this[3]
                )
                assertEquals(
                    TransactionView.SettingsChangeVariantQueued(
                        label = R.string.tx_list_disable_module,
                        status = AwaitingConfirmations,
                        statusText = R.string.tx_list_awaiting_confirmations,
                        statusColorRes = R.color.safe_pending_orange,
                        dateTimeText = "",
                        confirmations = 0,
                        threshold = 2,
                        confirmationsTextColor = R.color.medium_grey,
                        confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                        nonce = "1",
                        visibilityEllipsizedAddress = View.INVISIBLE,
                        visibilityModuleAddress = View.VISIBLE,
                        visibilityVersion = View.INVISIBLE,
                        address = defaultModuleAddress,
                        version = ""
                    ),
                    this[4]
                )
                assertEquals(
                    TransactionView.SettingsChangeVariantQueued(
                        label = R.string.tx_list_enable_module,
                        status = AwaitingExecution,
                        statusText = R.string.tx_list_awaiting_execution,
                        statusColorRes = R.color.safe_pending_orange,
                        dateTimeText = "",
                        confirmations = 2,
                        threshold = 2,
                        confirmationsTextColor = R.color.safe_green,
                        confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                        nonce = "1",
                        visibilityEllipsizedAddress = View.INVISIBLE,
                        visibilityModuleAddress = View.VISIBLE,
                        visibilityVersion = View.INVISIBLE,
                        address = defaultModuleAddress,
                        version = ""
                    ),
                    this[5]
                )
                assertEquals(
                    TransactionView.SectionHeader(title = R.string.tx_list_history),
                    this[6]
                )
                assertEquals(
                    TransactionView.SettingsChangeVariant(
                        label = R.string.tx_list_set_fallback_handler,
                        status = Cancelled,
                        statusText = R.string.tx_list_cancelled,
                        statusColorRes = R.color.dark_grey,
                        dateTimeText = "",
                        alpha = OPACITY_HALF,
                        visibilityEllipsizedAddress = View.VISIBLE,
                        visibilityModuleAddress = View.GONE,
                        visibilityVersion = View.VISIBLE,
                        address = defaultFallbackHandler,
                        version = "DefaultFallbackHandler"
                    ),
                    this[7]
                )
                assertEquals(
                    TransactionView.SettingsChangeVariant(
                        label = R.string.tx_list_change_mastercopy,
                        status = Success,
                        statusText = R.string.tx_list_success,
                        statusColorRes = R.color.safe_green,
                        dateTimeText = "",
                        address = SAFE_MASTER_COPY_1_0_0,
                        version = "1.0.0",
                        visibilityEllipsizedAddress = View.VISIBLE,
                        visibilityModuleAddress = View.GONE,
                        visibilityVersion = View.VISIBLE,
                        alpha = OPACITY_FULL
                    ),
                    this[8]
                )
                assertEquals(
                    TransactionView.SettingsChangeVariant(
                        label = R.string.tx_list_enable_module,
                        status = Failed,
                        statusText = R.string.tx_list_failed,
                        statusColorRes = R.color.safe_failed_red,
                        dateTimeText = "",
                        alpha = OPACITY_HALF,
                        version = "",
                        visibilityEllipsizedAddress = View.INVISIBLE,
                        visibilityModuleAddress = View.VISIBLE,
                        visibilityVersion = View.INVISIBLE,
                        address = defaultModuleAddress
                    ),
                    this[9]
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
        confirmations: Int? = 0,
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
        dataDecoded: DataDecodedDto = buildDataDecodedDto()
    ): Transaction =
        Transaction.SettingsChange(
            status = status,
            confirmations = confirmations,
            date = date,
            nonce = nonce,
            dataDecoded = dataDecoded
        )

    private fun buildDataDecodedDto(
        method: String = METHOD_REMOVE_OWNER,
        parameters: List<ParamsDto> = listOf()
    ): DataDecodedDto {
        return DataDecodedDto(
            method = method,
            parameters = parameters
        )
    }

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

