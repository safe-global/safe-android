package io.gnosis.safe.ui.transactions

import android.view.View
import androidx.paging.PagingData
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.models.*
import io.gnosis.data.models.TransactionStatus.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING
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
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_FULL
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_HALF
import io.gnosis.safe.ui.transactions.paging.TransactionPagingProvider
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.formatBackendDate
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
import java.util.*

class TransactionListViewModelTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private lateinit var transactionsViewModel: TransactionListViewModel

    private val safeRepository = mockk<SafeRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val transactionPagingProvider = mockk<TransactionPagingProvider>()

    private val balanceFormatter = BalanceFormatter()
    private val DS = balanceFormatter.decimalSeparator
    private val GS = balanceFormatter.groupingSeparator

    private val defaultSafeName: String = "Default Name"
    private val defaultSafeAddress: Solidity.Address = "0x1234".asEthereumAddress()!!
    private val defaultToAddress: Solidity.Address = "0x12345678".asEthereumAddress()!!
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
        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

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
    fun `init - (safeRepository failure) should emit ShowError`() {
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        val throwable = Throwable()
        coEvery { safeRepository.activeSafeFlow() } throws throwable
        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

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
    fun `load - (active safe with transactions) should emit LoadTransaction`() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val safeInfo = SafeInfo(
            safe.address, BigInteger.TEN, 2, emptyList(), Solidity.Address(BigInteger.ONE), emptyList(),
            Solidity.Address(BigInteger.ONE)
        )
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        coEvery { safeRepository.activeSafeFlow() } returns flow { emit(safe) }
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { transactionPagingProvider.getTransactionsStream(any(), any()) } returns flow { emit(PagingData.empty<Transaction>()) }
        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)

        with(testObserver.values()[0]) {
            assertEquals(false, isLoading)
            assertEquals(true, viewAction is LoadTransactions)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
            transactionPagingProvider.getTransactionsStream(safe.address, safeInfo)
        }
    }

    @Test
    fun `load - (transactionRepository failure) should emit ShowError`() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "test_safe")
        val safeInfo =
            SafeInfo(safe.address, BigInteger.TEN, 2, emptyList(), Solidity.Address(BigInteger.ONE), emptyList(), Solidity.Address(BigInteger.ONE))
        val testObserver = TestLiveDataObserver<TransactionsViewState>()
        val throwable = Throwable()
        coEvery { safeRepository.activeSafeFlow() } returns flow { emit(safe) }
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { transactionRepository.getTransactions(any()) } throws throwable
        coEvery { transactionPagingProvider.getTransactionsStream(any(), any()) } throws throwable
        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        transactionsViewModel.state.observeForever(testObserver)

        with(testObserver.values()[0]) {
            assertEquals(true, viewAction is BaseStateViewModel.ViewAction.ShowError)
            assertEquals(throwable, (viewAction as BaseStateViewModel.ViewAction.ShowError).error)
        }
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.getSafeInfo(safe.address)
        }
    }

    @Test
    fun `mapToTransactionView (tx list with no transfer) should map to empty list`() {

        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        val transactions = createEmptyTransactionList()
        val transactionViews =
            transactions.results.map {
                transactionsViewModel.getTransactionView(
                    it,
                    SafeInfo(
                        defaultSafeAddress,
                        defaultNonce,
                        defaultThreshold,
                        emptyList(),
                        Solidity.Address(BigInteger.ONE),
                        emptyList(),
                        Solidity.Address(BigInteger.ONE)
                    )
                )
            }

        assertEquals(0, transactionViews.size)
    }

    @Test
    fun `mapToTransactionView (tx list with queued transfers) should map to queued and ether transfer list`() {

        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        val transactions = createTransactionListWithStatus(
            PENDING,
            AWAITING_EXECUTION,
            AWAITING_CONFIRMATIONS
        )
        val transactionViews =
            transactions.results.map {
                transactionsViewModel.getTransactionView(
                    it,
                    SafeInfo(
                        defaultSafeAddress,
                        defaultNonce,
                        defaultThreshold,
                        emptyList(),
                        Solidity.Address(BigInteger.ONE),
                        emptyList(),
                        Solidity.Address(BigInteger.ONE)
                    )
                )
            }

        assertEquals(true, transactionViews[0] is TransactionView.TransferQueued)
        assertEquals(true, transactionViews[1] is TransactionView.TransferQueued)
        assertEquals(true, transactionViews[2] is TransactionView.TransferQueued)
    }

    @Test
    fun `mapTransactionView (tx list with queued and historic transfer) should map to queued and transfer list`() {

        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        val transactions = createTransactionListWithStatus(PENDING, SUCCESS)
        val transactionViews =
            transactions.results.map {
                transactionsViewModel.getTransactionView(
                    it,
                    SafeInfo(
                        defaultSafeAddress,
                        defaultNonce,
                        defaultThreshold,
                        emptyList(),
                        Solidity.Address(BigInteger.ONE),
                        emptyList(),
                        Solidity.Address(BigInteger.ONE)
                    )
                )
            }

        assertEquals(true, transactionViews[0] is TransactionView.TransferQueued)
        assertEquals(true, transactionViews[1] is TransactionView.Transfer)
    }

    @Test
    fun `mapTransactionView (tx list with queued and historic ether transfers) should map to queued and ether transfer list`() {

        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        val transactions = listOf(
            buildTransfer(
                status = AWAITING_CONFIRMATIONS,
                confirmations = 0,
                serviceTokenInfo = ETH_SERVICE_TOKEN_INFO,
                value = BigInteger.ZERO,
                recipient = defaultToAddress // outgoing
            ),
            buildTransfer(
                status = AWAITING_EXECUTION,
                confirmations = 2,
                serviceTokenInfo = ETH_SERVICE_TOKEN_INFO,
                value = BigInteger.ZERO,
                recipient = defaultSafeAddress // incoming
            ),
            buildTransfer(serviceTokenInfo = ETH_SERVICE_TOKEN_INFO, value = BigInteger("100000000000000"), status = FAILED),
            buildTransfer(serviceTokenInfo = ETH_SERVICE_TOKEN_INFO, value = BigInteger.ZERO, recipient = defaultToAddress),
            buildTransfer(serviceTokenInfo = ETH_SERVICE_TOKEN_INFO, value = BigInteger.ZERO, recipient = defaultSafeAddress),
            buildTransfer(
                serviceTokenInfo = ERC20_FALLBACK_SERVICE_TOKEN_INFO,
                value = BigInteger.TEN,
                recipient = defaultSafeAddress
            )
        )
        val transactionViews =
            transactions.map {
                transactionsViewModel.getTransactionView(
                    it,
                    SafeInfo(
                        defaultSafeAddress,
                        defaultNonce,
                        defaultThreshold,
                        emptyList(),
                        Solidity.Address(BigInteger.ONE),
                        emptyList(),
                        Solidity.Address(BigInteger.ONE)
                    )
                )
            }

        assertEquals(
            TransactionView.TransferQueued(
                id = "",
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_list_awaiting_confirmations,
                statusColorRes = R.color.safe_pending_orange,
                amountText = "0 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                address = defaultToAddress,
                confirmations = 0,
                nonce = "1",
                confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                confirmationsTextColor = R.color.medium_grey,
                threshold = 2
            ),
            transactionViews[0]
        )
        assertEquals(
            TransactionView.TransferQueued(
                id = "",
                status = AWAITING_EXECUTION,
                statusText = R.string.tx_list_awaiting_execution,
                statusColorRes = R.color.safe_pending_orange,
                amountText = "0 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                address = defaultFromAddress,
                threshold = 2,
                confirmationsTextColor = R.color.safe_green,
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
                statusText = R.string.tx_list_failed,
                statusColorRes = R.color.safe_failed_red,
                amountText = "-0${DS}0001 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                address = defaultToAddress,
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[2]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_list_success,
                statusColorRes = R.color.safe_green,
                amountText = "0 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                address = defaultToAddress,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[3]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_list_success,
                statusColorRes = R.color.safe_green,
                amountText = "0 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                address = defaultFromAddress,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[4]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_list_success,
                statusColorRes = R.color.safe_green,
                amountText = "+10 ERC20",
                amountColor = R.color.safe_green,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                address = defaultFromAddress,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[5]
        )
    }

    @Test
    fun `mapTransactionView (tx list with historic ether transfers) should map to ether transfer list`() {

        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        val transactions = listOf(
            buildTransfer(serviceTokenInfo = ERC20_FALLBACK_SERVICE_TOKEN_INFO, sender = defaultFromAddress, recipient = defaultSafeAddress),
            buildTransfer(serviceTokenInfo = ERC721_FALLBACK_SERVICE_TOKEN_INFO, sender = defaultFromAddress, recipient = defaultSafeAddress),
            buildTransfer(serviceTokenInfo = createErc20ServiceToken(), status = CANCELLED),
            buildTransfer(serviceTokenInfo = ETH_SERVICE_TOKEN_INFO, value = BigInteger("100000000000000"), status = FAILED)
        )
        val transactionViews =
            transactions.map {
                transactionsViewModel.getTransactionView(
                    it,
                    SafeInfo(
                        defaultSafeAddress,
                        defaultNonce,
                        defaultThreshold,
                        emptyList(),
                        Solidity.Address(BigInteger.ONE),
                        emptyList(),
                        Solidity.Address(BigInteger.ONE)
                    )
                )
            }

        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_list_success,
                statusColorRes = R.color.safe_green,
                amountText = "+1 ERC20",
                amountColor = R.color.safe_green,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                address = defaultFromAddress,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[0]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_list_success,
                statusColorRes = R.color.safe_green,
                amountText = "+1 NFT",
                amountColor = R.color.safe_green,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_green_10dp,
                address = defaultFromAddress,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[1]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = CANCELLED,
                statusText = R.string.tx_list_cancelled,
                statusColorRes = R.color.dark_grey,
                amountText = "-1 AQER",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                address = defaultToAddress,
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[2]
        )
        assertEquals(
            TransactionView.Transfer(
                id = "",
                status = FAILED,
                statusText = R.string.tx_list_failed,
                statusColorRes = R.color.safe_failed_red,
                amountText = "-0${DS}0001 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                txTypeIcon = R.drawable.ic_arrow_red_10dp,
                address = defaultToAddress,
                alpha = OPACITY_HALF,
                nonce = "1"
            ),
            transactionViews[3]
        )
    }

    @Test
    fun `mapTransactionView (tx list with historic custom txs) should map to custom transactions list`() {

        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        val transactions = listOf(
            buildCustom(status = AWAITING_EXECUTION, confirmations = 2),
            buildCustom(status = AWAITING_CONFIRMATIONS, confirmations = null),
            buildCustom(value = BigInteger("100000000000000"), address = defaultSafeAddress),
            buildCustom(status = FAILED),
            buildCustom(status = CANCELLED, value = BigInteger("100000000000000"))
        )
        val transactionViews =
            transactions.map {
                transactionsViewModel.getTransactionView(
                    it,
                    SafeInfo(
                        defaultSafeAddress,
                        defaultNonce,
                        defaultThreshold,
                        emptyList(),
                        Solidity.Address(BigInteger.ONE),
                        emptyList(),
                        Solidity.Address(BigInteger.ONE)
                    )
                )
            }

        assertEquals(
            TransactionView.CustomTransactionQueued(
                id = "",
                status = AWAITING_EXECUTION,
                statusText = R.string.tx_list_awaiting_execution,
                statusColorRes = R.color.safe_pending_orange,
                amountText = "0 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                address = defaultToAddress,
                dataSizeText = "0 bytes",
                nonce = "1",
                confirmationsIcon = R.drawable.ic_confirmations_green_16dp,
                confirmationsTextColor = R.color.safe_green,
                threshold = 2,
                confirmations = 2
            ),
            transactionViews[0]
        )
        assertEquals(
            TransactionView.CustomTransactionQueued(
                id = "",
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_list_awaiting_confirmations,
                statusColorRes = R.color.safe_pending_orange,
                amountText = "0 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                address = defaultToAddress,
                dataSizeText = "0 bytes",
                threshold = 2,
                confirmationsTextColor = R.color.medium_grey,
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
                statusText = R.string.tx_list_success,
                statusColorRes = R.color.safe_green,
                amountText = "+0${DS}0001 ETH",
                amountColor = R.color.safe_green,
                dateTimeText = Date(0).formatBackendDate(),
                address = defaultSafeAddress,
                alpha = OPACITY_FULL,
                dataSizeText = "0 bytes",
                nonce = "1"
            ),
            transactionViews[2]
        )
        assertEquals(
            TransactionView.CustomTransaction(
                id = "",
                status = FAILED,
                statusText = R.string.tx_list_failed,
                statusColorRes = R.color.safe_failed_red,
                amountText = "0 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                address = defaultToAddress,
                alpha = OPACITY_HALF,
                dataSizeText = "0 bytes",
                nonce = "1"
            ),
            transactionViews[3]
        )
        assertEquals(
            TransactionView.CustomTransaction(
                id = "",
                status = CANCELLED,
                statusText = R.string.tx_list_cancelled,
                statusColorRes = R.color.dark_grey,
                amountText = "-0${DS}0001 ETH",
                amountColor = R.color.gnosis_dark_blue,
                dateTimeText = Date(0).formatBackendDate(),
                address = defaultToAddress,
                alpha = OPACITY_HALF,
                dataSizeText = "0 bytes",
                nonce = "1"
            ),
            transactionViews[4]
        )
    }

    @Test
    fun `mapTransactionView (tx list with historic setting changes) should map to settings changes list`() {

        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        val transactions = listOf(
            // queued
            buildSettingsChange(
                status = AWAITING_EXECUTION,
                confirmations = 2,
                dataDecoded = buildDataDecodedDto(
                    METHOD_CHANGE_MASTER_COPY,
                    listOf(ParamsDto("_masterCopy", "address", SAFE_MASTER_COPY_1_1_1.asEthereumAddressString()))
                )
            ),
            buildSettingsChange(
                status = AWAITING_CONFIRMATIONS,
                dataDecoded = buildDataDecodedDto(METHOD_REMOVE_OWNER, listOf())
            ),
            buildSettingsChange(
                status = AWAITING_CONFIRMATIONS,
                dataDecoded = buildDataDecodedDto(METHOD_SET_FALLBACK_HANDLER, listOf())
            ),
            buildSettingsChange(
                status = AWAITING_CONFIRMATIONS,
                dataDecoded = buildDataDecodedDto(
                    METHOD_DISABLE_MODULE,
                    listOf(ParamsDto("module", "address", defaultModuleAddress.asEthereumAddressString()))
                )
            ),
            buildSettingsChange(
                status = AWAITING_EXECUTION,
                confirmations = 2,
                dataDecoded = buildDataDecodedDto(
                    METHOD_ENABLE_MODULE,
                    listOf(ParamsDto("module", "address", defaultModuleAddress.asEthereumAddressString()))
                )
            ),
            // history
            buildSettingsChange(
                status = CANCELLED,
                dataDecoded = buildDataDecodedDto(
                    METHOD_SET_FALLBACK_HANDLER,
                    listOf(ParamsDto("handler", "address", defaultFallbackHandler.asEthereumAddressString()))
                )
            ),
            buildSettingsChange(
                status = SUCCESS,
                confirmations = 2,
                dataDecoded = buildDataDecodedDto(
                    METHOD_CHANGE_MASTER_COPY,
                    listOf(ParamsDto("_masterCopy", "address", SAFE_MASTER_COPY_1_0_0.asEthereumAddressString()))
                )
            ),
            buildSettingsChange(
                status = FAILED,
                dataDecoded = buildDataDecodedDto(
                    METHOD_ENABLE_MODULE,
                    listOf(ParamsDto("module", "address", defaultModuleAddress.asEthereumAddressString()))
                )
            ),
            buildSettingsChange(
                status = SUCCESS,
                confirmations = 2,
                dataDecoded = buildDataDecodedDto(METHOD_REMOVE_OWNER, emptyList()),
                nonce = 10.toBigInteger()
            )
        )
        val transactionViews =
            transactions.map {
                transactionsViewModel.getTransactionView(
                    it,
                    SafeInfo(
                        defaultSafeAddress,
                        defaultNonce,
                        defaultThreshold,
                        emptyList(),
                        Solidity.Address(BigInteger.ONE),
                        emptyList(),
                        Solidity.Address(BigInteger.ONE)
                    )
                )
            }

        assertEquals(
            TransactionView.SettingsChangeVariantQueued(
                id = "",
                label = R.string.tx_list_change_mastercopy,
                status = AWAITING_EXECUTION,
                statusText = R.string.tx_list_awaiting_execution,
                statusColorRes = R.color.safe_pending_orange,
                dateTimeText = Date(0).formatBackendDate(),
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
            transactionViews[0]
        )
        assertEquals(
            TransactionView.SettingsChangeQueued(
                id = "",
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_list_awaiting_confirmations,
                statusColorRes = R.color.safe_pending_orange,
                dateTimeText = Date(0).formatBackendDate(),
                confirmations = 0,
                threshold = 2,
                confirmationsTextColor = R.color.medium_grey,
                confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                nonce = "1",
                settingNameText = "removeOwner"
            ),
            transactionViews[1]
        )
        assertEquals(
            TransactionView.SettingsChangeVariantQueued(
                id = "",
                label = R.string.tx_list_set_fallback_handler,
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_list_awaiting_confirmations,
                statusColorRes = R.color.safe_pending_orange,
                dateTimeText = Date(0).formatBackendDate(),
                confirmations = 0,
                threshold = 2,
                confirmationsTextColor = R.color.medium_grey,
                confirmationsIcon = R.drawable.ic_confirmations_grey_16dp,
                nonce = "1",
                version = DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING,
                address = null,
                visibilityVersion = View.VISIBLE,
                visibilityModuleAddress = View.GONE,
                visibilityEllipsizedAddress = View.VISIBLE
            ),
            transactionViews[2]
        )
        assertEquals(
            TransactionView.SettingsChangeVariantQueued(
                id = "",
                label = R.string.tx_list_disable_module,
                status = AWAITING_CONFIRMATIONS,
                statusText = R.string.tx_list_awaiting_confirmations,
                statusColorRes = R.color.safe_pending_orange,
                dateTimeText = Date(0).formatBackendDate(),
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
            transactionViews[3]
        )
        assertEquals(
            TransactionView.SettingsChangeVariantQueued(
                id = "",
                label = R.string.tx_list_enable_module,
                status = AWAITING_EXECUTION,
                statusText = R.string.tx_list_awaiting_execution,
                statusColorRes = R.color.safe_pending_orange,
                dateTimeText = Date(0).formatBackendDate(),
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
            transactionViews[4]
        )
        assertEquals(
            TransactionView.SettingsChangeVariant(
                id = "",
                label = R.string.tx_list_set_fallback_handler,
                status = CANCELLED,
                statusText = R.string.tx_list_cancelled,
                statusColorRes = R.color.dark_grey,
                dateTimeText = Date(0).formatBackendDate(),
                alpha = OPACITY_HALF,
                visibilityEllipsizedAddress = View.VISIBLE,
                visibilityModuleAddress = View.GONE,
                visibilityVersion = View.VISIBLE,
                address = defaultFallbackHandler,
                version = DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING,
                nonce = "1"
            ),
            transactionViews[5]
        )
        assertEquals(
            TransactionView.SettingsChangeVariant(
                id = "",
                label = R.string.tx_list_change_mastercopy,
                status = SUCCESS,
                statusText = R.string.tx_list_success,
                statusColorRes = R.color.safe_green,
                dateTimeText = Date(0).formatBackendDate(),
                address = SAFE_MASTER_COPY_1_0_0,
                version = "1.0.0",
                visibilityEllipsizedAddress = View.VISIBLE,
                visibilityModuleAddress = View.GONE,
                visibilityVersion = View.VISIBLE,
                alpha = OPACITY_FULL,
                nonce = "1"
            ),
            transactionViews[6]
        )
        assertEquals(
            TransactionView.SettingsChangeVariant(
                id = "",
                label = R.string.tx_list_enable_module,
                status = FAILED,
                statusText = R.string.tx_list_failed,
                statusColorRes = R.color.safe_failed_red,
                dateTimeText = Date(0).formatBackendDate(),
                alpha = OPACITY_HALF,
                version = "",
                visibilityEllipsizedAddress = View.INVISIBLE,
                visibilityModuleAddress = View.VISIBLE,
                visibilityVersion = View.INVISIBLE,
                address = defaultModuleAddress,
                nonce = "1"
            ),
            transactionViews[7]
        )
        assertEquals(
            TransactionView.SettingsChange(
                id = "",
                status = SUCCESS,
                statusText = R.string.tx_list_success,
                statusColorRes = R.color.safe_green,
                dateTimeText = Date(0).formatBackendDate(),
                method = METHOD_REMOVE_OWNER,
                alpha = OPACITY_FULL,
                nonce = "10"
            ),
            transactionViews[8]
        )
    }

    @Test
    fun `mapToTransactionView (tx list with creation tx) should map to list with creation tx`() {

        transactionsViewModel = TransactionListViewModel(transactionPagingProvider, safeRepository, balanceFormatter, appDispatchers)

        val transactions = createTransactionListWithCreationTx()
        val transactionViews =
            transactions.results.map {
                transactionsViewModel.getTransactionView(
                    it,
                    SafeInfo(
                        defaultSafeAddress,
                        defaultNonce,
                        defaultThreshold,
                        emptyList(),
                        Solidity.Address(BigInteger.ONE),
                        emptyList(),
                        Solidity.Address(BigInteger.ONE)
                    )
                )
            }

        assertEquals(true, transactionViews[0] is TransactionView.Creation)
        val creationTransactionView = transactionViews[0] as TransactionView.Creation
        assertEquals(SUCCESS, creationTransactionView.status)
        assertEquals(R.string.tx_list_success, creationTransactionView.statusText)
        assertEquals(R.string.tx_list_creation, creationTransactionView.label)
        assertEquals(Date(1).formatBackendDate(), creationTransactionView.dateTimeText)
        assertEquals(R.color.safe_green, creationTransactionView.statusColorRes)
        assertEquals("<random-id>", creationTransactionView.id)

    }

    private fun callVerification() {
        coVerify { safeRepository.getActiveSafe() }
        coVerify { safeRepository.getSafeInfo(defaultSafeAddress) }
        coVerify {
            transactionRepository.getTransactions(defaultSafeAddress)
        }
    }

    private fun createEmptyTransactionList(): Page<Transaction> {
        return Page(1, "", "", listOf())
    }

    private fun createTransactionListWithCreationTx(): Page<Transaction> {
        val transfers = listOf(
            Transaction.Creation(
                id = "<random-id>",
                status = SUCCESS,
                confirmations = 2,
                txInfo = TransactionInfo.Creation(
                    creator = defaultFromAddress,
                    factory = defaultToAddress,
                    implementation = defaultSafeAddress,
                    transactionHash = "0x00"
                ),
                timestamp = Date(1),
                executionInfo = DetailedExecutionInfo.MultisigExecutionDetails(
                    nonce = BigInteger.ZERO,
                    confirmations = listOf(
                        Confirmations(
                            signer = defaultFromAddress,
                            submittedAt = Date(2),
                            signature = ""
                        )
                    ),
                    confirmationsRequired = 1,
                    executor = defaultFromAddress,
                    safeTxHash = "0x00",
                    signers = listOf(defaultFromAddress),
                    submittedAt = Date(3)
                )
            )
        )

        return Page(1, "", "", transfers)
    }

    private fun createTransactionListWithStatus(vararg transactionStatus: TransactionStatus): Page<Transaction> {
        val transfers = transactionStatus.map { status ->
            Transaction.Transfer(
                id = "",
                status = status,
                confirmations = 2,
                recipient = defaultToAddress,
                sender = defaultFromAddress,
                value = BigInteger.ONE,
                date = Date(0),
                tokenInfo = ETH_SERVICE_TOKEN_INFO,
                nonce = defaultNonce,
                incoming = false
            )
        }
        return Page(1, "", "", transfers)
    }

    private fun buildTransfer(
        status: TransactionStatus = SUCCESS,
        confirmations: Int = 0,
        recipient: Solidity.Address = defaultToAddress,
        sender: Solidity.Address = defaultFromAddress,
        value: BigInteger = BigInteger.ONE,
        date: Date = Date(0),
        serviceTokenInfo: ServiceTokenInfo = ETH_SERVICE_TOKEN_INFO,
        nonce: BigInteger = defaultNonce
    ): Transaction =
        Transaction.Transfer(
            id = "",
            status = status,
            confirmations = confirmations,
            recipient = recipient,
            sender = sender,
            value = value,
            date = date,
            tokenInfo = serviceTokenInfo,
            nonce = nonce,
            incoming = defaultSafeAddress == recipient
        )

    private fun buildCustom(
        status: TransactionStatus = SUCCESS,
        confirmations: Int? = 0,
        value: BigInteger = BigInteger.ZERO,
        date: Date = Date(0),
        nonce: BigInteger = defaultNonce,
        address: Solidity.Address = defaultToAddress,
        dataSize: Int = 0
    ): Transaction =
        Transaction.Custom(
            id = "",
            status = status,
            confirmations = confirmations,
            value = value,
            date = date,
            nonce = nonce,
            address = address,
            dataSize = dataSize
        )

    private fun buildSettingsChange(
        status: TransactionStatus = SUCCESS,
        confirmations: Int = 0,
        date: Date = Date(0),
        nonce: BigInteger = defaultNonce,
        dataDecoded: DataDecodedDto = buildDataDecodedDto()
    ): Transaction =
        Transaction.SettingsChange(
            id = "",
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
}
