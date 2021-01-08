package io.gnosis.safe.ui.transactions

import androidx.paging.PagingData
import io.gnosis.data.BuildConfig
import io.gnosis.data.models.Page
import io.gnosis.data.models.Safe
import io.gnosis.data.models.assets.TokenInfo
import io.gnosis.data.models.assets.TokenType
import io.gnosis.data.models.transaction.*
import io.gnosis.data.models.transaction.TransactionStatus.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_MASTER_COPY
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_DISABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ENABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_REMOVE_OWNER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SET_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_IMPLEMENTATION_1_0_0
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_IMPLEMENTATION_1_1_1
import io.gnosis.data.repositories.TokenRepository.Companion.NATIVE_CURRENCY_INFO
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_FULL
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_HALF
import io.gnosis.safe.ui.transactions.paging.TransactionPagingProvider
import io.gnosis.safe.ui.transactions.paging.TransactionPagingSource
import io.gnosis.safe.utils.*
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import java.util.*

class TransactionListViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private lateinit var transactionListViewModel: TransactionListViewModel

    private val safeRepository = mockk<SafeRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val ownerRepository = mockk<OwnerCredentialsRepository>()
    private val transactionPagingProvider = mockk<TransactionPagingProvider>()

    private val balanceFormatter = BalanceFormatter()
    private val DS = balanceFormatter.decimalSeparator
    private val GS = balanceFormatter.groupingSeparator

    private val defaultSafeName: String = "Default Name"
    private val defaultSafeAddress: Solidity.Address = "0x1234".asEthereumAddress()!!
    private val defaultToAddress: Solidity.Address = "0x12345678".asEthereumAddress()!!
    private val defaultFactoryAddress: Solidity.Address = "0x12345678DeadBeef".asEthereumAddress()!!
    private val defaultFromAddress: Solidity.Address = "0x1234567890".asEthereumAddress()!!
    private val defaultModuleAddress: Solidity.Address = "0x25F73b24B866963B0e560fFF9bbA7908be0263E8".asEthereumAddress()!!
    private val defaultFallbackHandler: Solidity.Address = "0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44".asEthereumAddress()!!
    private val defaultSafe = Safe(defaultSafeAddress, defaultSafeName)
    private val defaultThreshold: Int = 2
    private val defaultNonce: BigInteger = BigInteger.ONE

    @Test
    fun `init - (no active safe change) should emit Loading`() {
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        coEvery { safeRepository.activeSafeFlow() } returns emptyFlow()
        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        transactionListViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(true, isLoading)
            assertEquals(null, viewAction)
        }
        coVerify(exactly = 1) { safeRepository.activeSafeFlow() }
        coVerify { transactionRepository wasNot Called }
    }

    @Test
    fun `init - (safeRepository failure) should emit ShowError`() {
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        val throwable = Throwable()
        coEvery { safeRepository.activeSafeFlow() } throws throwable
        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        transactionListViewModel.state.observeForever(testObserver)
        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(true, viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertEquals(throwable, (viewAction as BaseStateViewModel.ViewAction.ShowError).error)
        }
        coVerify(exactly = 1) { safeRepository.activeSafeFlow() }
        coVerify { transactionRepository wasNot Called }
    }

    @Test
    fun `init - (active safe with transactions) should emit ActiveSafeChanged`() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        coEvery { safeRepository.activeSafeFlow() } returns flow { emit(safe) }
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { ownerRepository.retrieveCredentials() } returns null
        coEvery {
            transactionPagingProvider.getTransactionsStream(
                any(),
                TransactionPagingSource.Type.HISTORY
            )
        } returns flow { emit(PagingData.empty()) }
        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        transactionListViewModel.state.observeForever(testObserver)

        testObserver.assertValueCount(1)
        with(testObserver.values()[0]) {
            assertEquals(true, viewAction is ActiveSafeChanged)
            assertEquals(true, isLoading)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
        }
    }

    @Test
    fun `load - (transactionRepository failure) should emit ShowError`() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        val throwable = Throwable()
        coEvery { safeRepository.activeSafeFlow() } returns flow { emit(safe) }
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { ownerRepository.retrieveCredentials() } returns null
        coEvery { transactionRepository.getHistoryTransactions(any()) } throws throwable
        coEvery { transactionPagingProvider.getTransactionsStream(any(), TransactionPagingSource.Type.HISTORY) } throws throwable
        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)
        transactionListViewModel.load(TransactionPagingSource.Type.HISTORY)

        transactionListViewModel.state.observeForever(testObserver)

        with(testObserver.values()[0]) {
            assertEquals(true, viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertEquals(throwable, (viewAction as BaseStateViewModel.ViewAction.ShowError).error)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address) wasNot Called
        }
    }

    @Test
    fun `mapToTransactionView (tx list with no transfer) should map to empty list`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = createEmptyTransactionList()
        val transactionViews = transactions.results.map { transactionListViewModel.getTransactionView(it, defaultSafeAddress) }

        assertEquals(0, transactionViews.size)
    }

    @Test
    fun `mapToTransactionView (tx list with queued transfers) should map to queued and ether transfer list`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = createTransactionListWithStatus(
            PENDING,
            AWAITING_EXECUTION,
            AWAITING_CONFIRMATIONS
        )
        val transactionViews = transactions.results.map { transaction ->
            transactionListViewModel.getTransactionView(
                transaction, defaultSafeAddress,
                awaitingYourConfirmation = false,
                isConflict = false
            )
        }

        assertEquals("1", (transactionViews[0] as TransactionView.TransferQueued).nonce)
        assertEquals("1", (transactionViews[1] as TransactionView.TransferQueued).nonce)
        assertEquals("1", (transactionViews[2] as TransactionView.TransferQueued).nonce)
    }

    @Test
    fun `mapToTransactionView (tx list with conflicting queued transfers) should map to queued and ether transfer list`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = createTransactionListWithStatus(
            PENDING,
            AWAITING_EXECUTION,
            AWAITING_CONFIRMATIONS
        )
        val transactionViews = transactions.results.map { transaction ->
            transactionListViewModel.getTransactionView(
                transaction = transaction,
                activeSafe = defaultSafeAddress,
                awaitingYourConfirmation = false,
                isConflict = true
            )
        }

        assertEquals("", (transactionViews[0] as TransactionView.TransferQueued).nonce)
        assertEquals("", (transactionViews[1] as TransactionView.TransferQueued).nonce)
        assertEquals("", (transactionViews[2] as TransactionView.TransferQueued).nonce)
    }

    @Test
    fun `mapTransactionView (tx list with queued and historic transfer) should map to queued and transfer list`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = createTransactionListWithStatus(PENDING, SUCCESS)
        val transactionViews = transactions.results.map { transactionListViewModel.getTransactionView(it, defaultSafeAddress) }

        assertEquals(true, transactionViews[0] is TransactionView.TransferQueued)
        assertEquals(true, transactionViews[1] is TransactionView.Transfer)
    }

    @Test
    fun `mapTransactionView (tx list with queued and historic ETH transfers) should map to queued and ETH transfer list`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = listOf(
            buildTransfer(
                status = AWAITING_CONFIRMATIONS,
                confirmations = 0,
                serviceTokenInfo = NATIVE_CURRENCY_INFO,
                value = BigInteger.ZERO,
                recipient = defaultToAddress // outgoing
            ),
            buildTransfer(
                status = AWAITING_EXECUTION,
                confirmations = 2,
                serviceTokenInfo = NATIVE_CURRENCY_INFO,
                value = BigInteger.ZERO,
                recipient = defaultSafeAddress // incoming
            ),
            buildTransfer(serviceTokenInfo = NATIVE_CURRENCY_INFO, value = BigInteger("100000000000000"), status = FAILED),
            buildTransfer(serviceTokenInfo = NATIVE_CURRENCY_INFO, value = BigInteger.ZERO, recipient = defaultToAddress),
            buildTransfer(serviceTokenInfo = NATIVE_CURRENCY_INFO, value = BigInteger.ZERO, recipient = defaultSafeAddress),
            buildTransfer(
                serviceTokenInfo = ERC20_TOKEN_INFO_NO_SYMBOL,
                value = BigInteger.TEN,
                recipient = defaultSafeAddress
            )
        )
        val transactionViews = transactions.map { transactionListViewModel.getTransactionView(it, defaultSafeAddress) }

        assertEquals(
            TransactionView.TransferQueued(
                id = "",
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_status_awaiting_confirmations,
                statusColorRes = R.color.secondary,
                amountText = "0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}",
                amountColor = R.color.text_emphasis_high,
                dateTime = Date(0),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                direction = R.string.tx_list_send,
                confirmations = 0,
                nonce = "1",
                confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                confirmationsTextColor = R.color.text_emphasis_low,
                threshold = 2
            ),
            transactionViews[0]
        )
        assertEquals(
            TransactionView.TransferQueued(
                id = "",
                status = AWAITING_EXECUTION,
                statusText = R.string.tx_status_awaiting_execution,
                statusColorRes = R.color.secondary,
                amountText = "0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}",
                amountColor = R.color.text_emphasis_high,
                dateTime = Date(0),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                direction = R.string.tx_list_receive,
                threshold = 2,
                confirmationsTextColor = R.color.primary,
                confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                nonce = "1",
                confirmations = 2
            ),
            transactionViews[1]
        )
        //FIXME: pass id's
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = FAILED,
                statusText = R.string.tx_status_failed,
                statusColorRes = R.color.error,
                amountText = "-0${DS}0001 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}",
                amountColor = R.color.text_emphasis_high,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                direction = R.string.tx_list_send,
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[2]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_status_success,
                statusColorRes = R.color.primary,
                amountText = "0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}",
                amountColor = R.color.text_emphasis_high,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                direction = R.string.tx_list_send,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[3]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_status_success,
                statusColorRes = R.color.primary,
                amountText = "0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}",
                amountColor = R.color.text_emphasis_high,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                direction = R.string.tx_list_receive,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[4]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_status_success,
                statusColorRes = R.color.primary,
                amountText = "+10 ERC20",
                amountColor = R.color.primary,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                direction = R.string.tx_list_receive,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[5]
        )
    }

    @Test
    fun `mapTransactionView (tx list with historic ether transfers) should map to ether transfer list`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = listOf(
            buildTransfer(serviceTokenInfo = ERC20_TOKEN_INFO_NO_SYMBOL, sender = defaultFromAddress, recipient = defaultSafeAddress),
            buildTransfer(serviceTokenInfo = ERC721_TOKEN_INFO_NO_SYMBOL, sender = defaultFromAddress, recipient = defaultSafeAddress),
            buildTransfer(serviceTokenInfo = createErc20TokenInfo(), status = CANCELLED),
            buildTransfer(serviceTokenInfo = NATIVE_CURRENCY_INFO, value = BigInteger("100000000000000"), status = FAILED)
        )
        val transactionViews = transactions.map { transactionListViewModel.getTransactionView(it, defaultSafeAddress) }

        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_status_success,
                statusColorRes = R.color.primary,
                amountText = "+1 ERC20",
                amountColor = R.color.primary,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                direction = R.string.tx_list_receive,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[0]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_status_success,
                statusColorRes = R.color.primary,
                amountText = "+1 NFT",
                amountColor = R.color.primary,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                direction = R.string.tx_list_receive,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[1]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = CANCELLED,
                statusText = R.string.tx_status_cancelled,
                statusColorRes = R.color.text_emphasis_medium,
                amountText = "-1 AQER",
                amountColor = R.color.text_emphasis_high,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                direction = R.string.tx_list_send,
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[2]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = FAILED,
                statusText = R.string.tx_status_failed,
                statusColorRes = R.color.error,
                amountText = "-0${DS}0001 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}",
                amountColor = R.color.text_emphasis_high,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                direction = R.string.tx_list_send,
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[3]
        )
    }

    @Test
    fun `mapTransactionView (tx list with historic custom txs) should map to custom transactions list`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = listOf(
            buildCustom(status = AWAITING_EXECUTION, confirmations = 2),
            buildCustom(status = AWAITING_CONFIRMATIONS),
            buildCustom(value = BigInteger("100000000000000"), address = defaultSafeAddress),
            buildCustom(status = FAILED),
            buildCustom(status = CANCELLED, value = BigInteger("100000000000000"))
        )
        val transactionViews = transactions.map { transactionListViewModel.getTransactionView(it, defaultSafeAddress) }

        assertEquals(
            TransactionView.CustomTransactionQueued(
                id = "",
                status = AWAITING_EXECUTION,
                statusText = R.string.tx_status_awaiting_execution,
                statusColorRes = R.color.secondary,
                dateTime = Date(0),
                methodName = "multiSend",
                nonce = "1",
                confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                confirmationsTextColor = R.color.primary,
                threshold = 2,
                confirmations = 2
            ),
            transactionViews[0]
        )
        assertEquals(
            TransactionView.CustomTransactionQueued(
                id = "",
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_status_awaiting_confirmations,
                statusColorRes = R.color.secondary,
                dateTime = Date(0),
                methodName = "multiSend",
                threshold = 2,
                confirmationsTextColor = R.color.text_emphasis_low,
                confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                nonce = "1",
                confirmations = 0
            ),
            transactionViews[1]
        )
        assertEquals(
            TransactionView.CustomTransaction(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_status_success,
                statusColorRes = R.color.primary,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                methodName = "multiSend",
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[2]
        )
        assertEquals(
            TransactionView.CustomTransaction(
                id = "",
                status = FAILED,
                statusText = R.string.tx_status_failed,
                statusColorRes = R.color.error,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                methodName = "multiSend",
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[3]
        )
        assertEquals(
            TransactionView.CustomTransaction(
                id = "",
                status = CANCELLED,
                statusText = R.string.tx_status_cancelled,
                statusColorRes = R.color.text_emphasis_medium,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                methodName = "multiSend",
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[4]
        )
    }

    @Test
    fun `mapTransactionView (tx list with historic setting changes) should map to settings changes list`() {
        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = listOf(
            // queued
            buildSettingsChange(
                status = AWAITING_EXECUTION,
                confirmations = 2,
                dataDecoded = buildDataDecodedDto(
                    METHOD_CHANGE_MASTER_COPY,
                    listOf(Param.Address("address", "_masterCopy", SAFE_IMPLEMENTATION_1_1_1))
                ),
                settingsInfo = SettingsInfo.ChangeImplementation(SAFE_IMPLEMENTATION_1_1_1)
            ),
            buildSettingsChange(
                status = AWAITING_CONFIRMATIONS,
                dataDecoded = buildDataDecodedDto(METHOD_REMOVE_OWNER, listOf()),
                settingsInfo = SettingsInfo.RemoveOwner(defaultSafeAddress, 1)
            ),
            buildSettingsChange(
                status = AWAITING_CONFIRMATIONS,
                dataDecoded = buildDataDecodedDto(METHOD_SET_FALLBACK_HANDLER, listOf()),
                settingsInfo = null
            ),
            buildSettingsChange(
                status = AWAITING_CONFIRMATIONS,
                dataDecoded = buildDataDecodedDto(
                    METHOD_DISABLE_MODULE,
                    listOf(Param.Address("address", "module", defaultModuleAddress))
                ),
                settingsInfo = SettingsInfo.DisableModule(defaultModuleAddress)
            ),
            buildSettingsChange(
                status = AWAITING_EXECUTION,
                confirmations = 2,
                dataDecoded = buildDataDecodedDto(
                    METHOD_ENABLE_MODULE,
                    listOf(Param.Address("address", "module", defaultModuleAddress))
                ),
                settingsInfo = SettingsInfo.EnableModule(defaultModuleAddress)
            ),
            // history
            buildSettingsChange(
                status = CANCELLED,
                dataDecoded = buildDataDecodedDto(
                    METHOD_SET_FALLBACK_HANDLER,
                    listOf(Param.Address("address", "handler", defaultFallbackHandler))
                ),
                settingsInfo = SettingsInfo.SetFallbackHandler(defaultFallbackHandler)
            ),
            buildSettingsChange(
                status = SUCCESS,
                confirmations = 2,
                dataDecoded = buildDataDecodedDto(
                    METHOD_CHANGE_MASTER_COPY,
                    listOf(Param.Address("address", "_masterCopy", SAFE_IMPLEMENTATION_1_0_0))
                ),
                settingsInfo = SettingsInfo.ChangeImplementation(SAFE_IMPLEMENTATION_1_0_0)
            ),
            buildSettingsChange(
                status = FAILED,
                dataDecoded = buildDataDecodedDto(
                    METHOD_ENABLE_MODULE,
                    listOf(Param.Address("address", "module", defaultModuleAddress))
                ),
                settingsInfo = SettingsInfo.EnableModule(defaultModuleAddress)
            ),
            buildSettingsChange(
                status = SUCCESS,
                confirmations = 2,
                dataDecoded = buildDataDecodedDto(METHOD_REMOVE_OWNER, emptyList()),
                nonce = 10.toBigInteger(),
                settingsInfo = SettingsInfo.RemoveOwner(defaultSafeAddress, 1)
            )
        )
        val transactionViews = transactions.map { transactionListViewModel.getTransactionView(it, defaultSafeAddress) }

        assertEquals(
            TransactionView.SettingsChangeQueued(
                id = "",
                status = AWAITING_EXECUTION,
                statusText = R.string.tx_status_awaiting_execution,
                statusColorRes = R.color.secondary,
                dateTime = Date(0),
                confirmations = 2,
                threshold = 2,
                confirmationsTextColor = R.color.primary,
                confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                nonce = "1",
                method = "changeMasterCopy"
            ),
            transactionViews[0]
        )
        assertEquals(
            TransactionView.SettingsChangeQueued(
                id = "",
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_status_awaiting_confirmations,
                statusColorRes = R.color.secondary,
                dateTime = Date(0),
                method = "removeOwner",
                confirmations = 0,
                threshold = 2,
                confirmationsTextColor = R.color.text_emphasis_low,
                confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                nonce = "1"
            ),
            transactionViews[1]
        )
        assertEquals(
            TransactionView.SettingsChangeQueued(
                id = "",
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_status_awaiting_confirmations,
                statusColorRes = R.color.secondary,
                dateTime = Date(0),
                confirmations = 0,
                threshold = 2,
                confirmationsTextColor = R.color.text_emphasis_low,
                confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                nonce = "1",
                method = "setFallbackHandler"
            ),
            transactionViews[2]
        )
        assertEquals(
            TransactionView.SettingsChangeQueued(
                id = "",
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_status_awaiting_confirmations,
                statusColorRes = R.color.secondary,
                dateTime = Date(0),
                confirmations = 0,
                threshold = 2,
                confirmationsTextColor = R.color.text_emphasis_low,
                confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                nonce = "1",
                method = "disableModule"
            ),
            transactionViews[3]
        )
        assertEquals(
            TransactionView.SettingsChangeQueued(
                id = "",
                status = AWAITING_EXECUTION,
                statusText = R.string.tx_status_awaiting_execution,
                statusColorRes = R.color.secondary,
                dateTime = Date(0),
                confirmations = 2,
                threshold = 2,
                confirmationsTextColor = R.color.primary,
                confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                nonce = "1",
                method = "enableModule"
            ),
            transactionViews[4]
        )
        assertEquals(
            TransactionView.SettingsChange(
                id = "",
                status = CANCELLED,
                statusText = R.string.tx_status_cancelled,
                statusColorRes = R.color.text_emphasis_medium,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                method = "setFallbackHandler",
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[5]
        )
        assertEquals(
            TransactionView.SettingsChange(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_status_success,
                statusColorRes = R.color.primary,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                method = "changeMasterCopy",
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[6]
        )
        assertEquals(
            TransactionView.SettingsChange(
                id = "",
                status = FAILED,
                statusText = R.string.tx_status_failed,
                statusColorRes = R.color.error,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                method = "enableModule",
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[7]
        )
        assertEquals(
            TransactionView.SettingsChange(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_status_success,
                statusColorRes = R.color.primary,
                dateTimeText = Date(0).formatBackendTimeOfDay(),
                method = METHOD_REMOVE_OWNER,
                alpha = OPACITY_FULL,
                nonce = "10"
            ),
            transactionViews[8]
        )
    }

    @Test
    fun `mapToTransactionView (tx list with creation tx) should map to list with creation tx`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val transactions = createTransactionListWithCreationTx()
        val transactionViews = transactions.results.map { transactionListViewModel.getTransactionView(it, defaultSafeAddress) }

        assertEquals(true, transactionViews[0] is TransactionView.Creation)
        val creationTransactionView = transactionViews[0] as TransactionView.Creation
        assertEquals(SUCCESS, creationTransactionView.status)
        assertEquals(R.string.tx_status_success, creationTransactionView.statusText)
        assertEquals(R.string.tx_list_creation, creationTransactionView.label)
        assertEquals(Date(1).formatBackendTimeOfDay(), creationTransactionView.dateTimeText)
        assertEquals(R.color.primary, creationTransactionView.statusColorRes)
        assertEquals("<random-id>", creationTransactionView.id)
        assertEquals(defaultFactoryAddress.asEthereumAddressString(), creationTransactionView.creationDetails?.factory)
        assertEquals(defaultFromAddress.asEthereumAddressString(), creationTransactionView.creationDetails?.creator)
        assertEquals(defaultSafeAddress.asEthereumAddressString(), creationTransactionView.creationDetails?.implementation)
    }

    @Test
    fun `mapToTransactionView (tx list with awaiting confirmation transactions) should map to list with items having correct awaiting confirmation string`() {

        transactionListViewModel =
            TransactionListViewModel(transactionPagingProvider, safeRepository, ownerRepository, balanceFormatter, appDispatchers)

        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val owner = OwnerCredentials(Solidity.Address(BigInteger.ONE), BigInteger.ONE)
        val notOwner = OwnerCredentials(Solidity.Address(BigInteger.TEN), BigInteger.TEN)

        val transactions = listOf(
            buildTransfer(
                status = AWAITING_CONFIRMATIONS,
                missingSigners = listOf(owner.address)
            ),
            buildTransfer(
                status = AWAITING_CONFIRMATIONS,
                missingSigners = listOf(notOwner.address)
            ),
            buildSettingsChange(
                status = AWAITING_CONFIRMATIONS,
                missingSigners = listOf(owner.address)
            ),
            buildSettingsChange(
                status = AWAITING_CONFIRMATIONS,
                missingSigners = listOf(notOwner.address)
            ),
            buildCustom(
                status = AWAITING_CONFIRMATIONS,
                missingSigners = listOf(owner.address)
            ),
            buildCustom(
                status = AWAITING_CONFIRMATIONS,
                missingSigners = listOf(notOwner.address)
            ),
            buildCustom(
                status = AWAITING_EXECUTION,
                missingSigners = listOf(owner.address)
            )
        )

        val transactionViewData = transactions.map {
            transactionListViewModel.getTransactionView(it, safe.address, it.canBeSignedByOwner(owner.address))
        }

        assertTrue(transactionViewData[0] is TransactionView.TransferQueued)
        assertEquals(R.string.tx_status_awaiting_your_confirmation, (transactionViewData[0] as TransactionView.TransferQueued).statusText)

        assertTrue(transactionViewData[1] is TransactionView.TransferQueued)
        assertEquals(R.string.tx_status_awaiting_confirmations, (transactionViewData[1] as TransactionView.TransferQueued).statusText)

        assertTrue(transactionViewData[2] is TransactionView.SettingsChangeQueued)
        assertEquals(R.string.tx_status_awaiting_your_confirmation, (transactionViewData[2] as TransactionView.SettingsChangeQueued).statusText)

        assertTrue(transactionViewData[3] is TransactionView.SettingsChangeQueued)
        assertEquals(R.string.tx_status_awaiting_confirmations, (transactionViewData[3] as TransactionView.SettingsChangeQueued).statusText)

        assertTrue(transactionViewData[4] is TransactionView.CustomTransactionQueued)
        assertEquals(R.string.tx_status_awaiting_your_confirmation, (transactionViewData[4] as TransactionView.CustomTransactionQueued).statusText)

        assertTrue(transactionViewData[5] is TransactionView.CustomTransactionQueued)
        assertEquals(R.string.tx_status_awaiting_confirmations, (transactionViewData[5] as TransactionView.CustomTransactionQueued).statusText)

        assertTrue(transactionViewData[6] is TransactionView.CustomTransactionQueued)
        assertEquals(R.string.tx_status_awaiting_execution, (transactionViewData[6] as TransactionView.CustomTransactionQueued).statusText)
    }

    @Test
    fun `incoming (Transfer_TransactionDirection_INCOMING) is true`() {
        val transfer = TransactionInfo.Transfer(
            sender = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
            recipient = "0x938bae50a210b80EA233112800Cd5Bc2e7644300".asEthereumAddress()!!,
            transferInfo = TransferInfo.EtherTransfer(BigInteger.ONE),
            direction = TransactionDirection.INCOMING
        )

        val actual = transfer.incoming()

        assertEquals(true, actual)
    }

    @Test
    fun `incoming (Transfer_TransactionDirection_UNKNOWN) is true`() {
        val transfer = TransactionInfo.Transfer(
            sender = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
            recipient = "0x938bae50a210b80EA233112800Cd5Bc2e7644300".asEthereumAddress()!!,
            transferInfo = TransferInfo.EtherTransfer(BigInteger.ONE),
            direction = TransactionDirection.UNKNOWN
        )

        val actual = transfer.incoming()

        assertEquals(true, actual)
    }

    @Test
    fun `incoming (Transfer_TransactionDirection_OUTGOING) is true`() {
        val transfer = TransactionInfo.Transfer(
            sender = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!,
            recipient = "0x938bae50a210b80EA233112800Cd5Bc2e7644300".asEthereumAddress()!!,
            transferInfo = TransferInfo.EtherTransfer(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING
        )

        val actual = transfer.incoming()

        assertEquals(false, actual)
    }

    private fun createEmptyTransactionList(): Page<Transaction> {
        return Page(1, "", "", listOf())
    }

    private fun createTransactionListWithCreationTx(): Page<Transaction> {
        val transfers = listOf(
            Transaction(
                id = "<random-id>",
                txStatus = SUCCESS,
                txInfo = TransactionInfo.Creation(
                    creator = defaultFromAddress,
                    factory = defaultFactoryAddress,
                    implementation = defaultSafeAddress,
                    transactionHash = "0x00"
                ),
                timestamp = Date(1),
                executionInfo = ExecutionInfo(
                    nonce = BigInteger.ZERO,
                    confirmationsSubmitted = 1,
                    confirmationsRequired = 1,
                    missingSigners = emptyList()
                )
            )
        )

        return Page(1, "", "", transfers)
    }

    private fun createTransactionListWithStatus(vararg transactionStatus: TransactionStatus): Page<Transaction> {
        val transfers = transactionStatus.map { status ->
            Transaction(
                id = "",
                txStatus = status,
                executionInfo = ExecutionInfo(nonce = defaultNonce, confirmationsRequired = 3, confirmationsSubmitted = 2, missingSigners = null),
                txInfo = TransactionInfo.Transfer(
                    recipient = defaultToAddress,
                    sender = defaultFromAddress,
                    direction = TransactionDirection.OUTGOING,
                    transferInfo = TransferInfo.EtherTransfer(BigInteger.ONE)
                ),
                timestamp = Date(0)
            )
        }
        return Page(1, "", "", transfers)
    }

    private fun buildTransfer(
        status: TransactionStatus = SUCCESS,
        confirmations: Int = 0,
        missingSigners: List<Solidity.Address>? = null,
        recipient: Solidity.Address = defaultToAddress,
        sender: Solidity.Address = defaultFromAddress,
        value: BigInteger = BigInteger.ONE,
        date: Date = Date(0),
        serviceTokenInfo: TokenInfo = NATIVE_CURRENCY_INFO,
        nonce: BigInteger = defaultNonce
    ): Transaction =
        Transaction(
            id = "",
            txStatus = status,
            txInfo = TransactionInfo.Transfer(
                recipient = recipient,
                sender = sender,
                direction = if (defaultSafeAddress == recipient) TransactionDirection.INCOMING else TransactionDirection.OUTGOING,
                transferInfo = transferInfoFromToken(tokenInfo = serviceTokenInfo, value = value)
            ),
            executionInfo = ExecutionInfo(
                nonce = nonce,
                confirmationsRequired = defaultThreshold,
                confirmationsSubmitted = confirmations,
                missingSigners = missingSigners
            ),
            timestamp = date
        )

    private fun transferInfoFromToken(tokenInfo: TokenInfo, value: BigInteger): TransferInfo {
        return when (tokenInfo.tokenType) {
            TokenType.ERC20 -> TransferInfo.Erc20Transfer(
                tokenAddress = tokenInfo.address,
                tokenName = tokenInfo.name,
                tokenSymbol = tokenInfo.symbol,
                logoUri = tokenInfo.logoUri,
                decimals = tokenInfo.decimals,
                value = value
            )
            TokenType.ERC721 -> TransferInfo.Erc721Transfer(
                tokenAddress = tokenInfo.address,
                tokenName = tokenInfo.name,
                tokenSymbol = tokenInfo.symbol,
                logoUri = tokenInfo.logoUri,
                tokenId = "tokenId"
            )
            else -> TransferInfo.EtherTransfer(value)
        }
    }

    private fun buildCustom(
        status: TransactionStatus = SUCCESS,
        confirmations: Int = 0,
        missingSigners: List<Solidity.Address>? = null,
        value: BigInteger = BigInteger.ZERO,
        date: Date = Date(0),
        nonce: BigInteger = defaultNonce,
        address: Solidity.Address = defaultToAddress,
        dataSize: Int = 0
    ): Transaction =
        Transaction(
            id = "",
            txStatus = status,
            txInfo = TransactionInfo.Custom(to = address, dataSize = dataSize, value = value, methodName = "multiSend"),
            executionInfo = ExecutionInfo(
                nonce = nonce,
                confirmationsRequired = defaultThreshold,
                confirmationsSubmitted = confirmations,
                missingSigners = missingSigners
            ),
            timestamp = date
        )

    private fun buildSettingsChange(
        status: TransactionStatus = SUCCESS,
        confirmations: Int = 0,
        missingSigners: List<Solidity.Address>? = null,
        date: Date = Date(0),
        nonce: BigInteger = defaultNonce,
        dataDecoded: DataDecoded = buildDataDecodedDto(),
        settingsInfo: SettingsInfo? = null
    ): Transaction =
        Transaction(
            id = "",
            txStatus = status,
            txInfo = TransactionInfo.SettingsChange(dataDecoded = dataDecoded, settingsInfo = settingsInfo),
            executionInfo = ExecutionInfo(
                nonce = nonce,
                confirmationsRequired = defaultThreshold,
                confirmationsSubmitted = confirmations,
                missingSigners = missingSigners
            ),
            timestamp = date
        )

    private fun buildDataDecodedDto(
        method: String = METHOD_REMOVE_OWNER,
        parameters: List<Param> = listOf()
    ): DataDecoded {
        return DataDecoded(
            method = method,
            parameters = parameters
        )
    }

    private fun createErc20TokenInfo() = TokenInfo(
        tokenType = TokenType.ERC20,
        address = "0x63704B63Ac04f3a173Dfe677C7e3D330c347CD88".asEthereumAddress()!!,
        decimals = 0,
        name = "TEST AQER",
        symbol = "AQER",
        logoUri = "local::native_currency"
    )

    companion object {

        private val ERC20_TOKEN_INFO_NO_SYMBOL = TokenInfo(
            TokenType.ERC20,
            Solidity.Address(BigInteger.ZERO),
            0,
            "",
            "ERC20",
            "local::native_currency"
        )
        private val ERC721_TOKEN_INFO_NO_SYMBOL = TokenInfo(
            TokenType.ERC721,
            Solidity.Address(BigInteger.ZERO),
            0,
            "",
            "",
            "local::native_currency"
        )
    }
}
