package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(MockitoJUnitRunner::class)
class SafeTransactionsAdapterHeaderViewHolderTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private val computationScheduler = TestScheduler()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var itemView: View

    @Mock
    private lateinit var timestampTextView: TextView

    @Mock
    private lateinit var targetTextView: TextView

    @Mock
    private lateinit var typeIconImageView: ImageView

    @Mock
    private lateinit var typeImageView: ImageView

    @Mock
    private lateinit var valueTextView: TextView

    @Mock
    private lateinit var infoTextView: TextView

    @Mock
    private lateinit var targetImageView: BlockiesImageView

    @Mock
    private lateinit var progressView: MaterialProgressBar

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    @Mock
    private lateinit var viewModel: SafeTransactionsContract

    @Mock
    private lateinit var transactionSubject: Subject<String>

    private lateinit var addressHelper: AddressHelper
    private lateinit var viewHolder: SafeTransactionsAdapter.TransactionViewHolder

    @Before
    fun setUp() {
        RxJavaPlugins.setComputationSchedulerHandler { _ -> computationScheduler }
        given(itemView.context).willReturn(context)
        addressHelper = AddressHelper(addressBookRepository)
        viewHolder = SafeTransactionsAdapter.TransactionViewHolder(
            addressHelper,
            viewModel,
            transactionSubject,
            itemView
        )
    }

    private fun testBindTransactionEntryAssetTransfer(tokenInfo: Single<ERC20Token>, valueText: String) {
        context.mockGetString()
        context.mockGetColor()
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_timestamp, timestampTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_label, targetTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_type_icon, typeIconImageView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_value, valueTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_info, infoTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_image, targetImageView)

        given(viewModel.loadTransactionInfo(anyString())).willReturn(
            Single.just(
                TransactionInfo(
                    "id_1",
                    "chain_hash",
                    TEST_SAFE,
                    TransactionData.AssetTransfer(TEST_TOKEN.address, BigInteger("1000000"), TEST_ADDRESS),
                    TEST_TIMESTAMP,
                    TEST_GAS_LIMIT,
                    TEST_GAS_PRICE,
                    ERC20Token.ETHER_TOKEN.address
                )
            )
        )
        given(viewModel.observeTransactionStatus(anyString())).willReturn(Observable.empty())
        given(viewModel.loadTokenInfo(MockUtils.any())).willReturn(tokenInfo)
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_ADDRESS, "Test Name", "")))

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("id_1"), emptyList())

        then(timestampTextView).should().text = R.string.loading.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(targetTextView).shouldHaveZeroInteractions()

        var rxClickListener: View.OnClickListener? = null
        given(itemView.setOnClickListener(MockUtils.any())).will {
            rxClickListener = it.arguments.first() as View.OnClickListener
            Unit
        }

        viewHolder.start()

        assertNotNull(rxClickListener)

        then(typeIconImageView).should().setImageResource(R.drawable.ic_transaction_outgoing)
        then(typeIconImageView).shouldHaveNoMoreInteractions()
        then(valueTextView).should().text = null
        then(valueTextView).should().text = valueText
        then(valueTextView).should().visibility = View.GONE
        then(valueTextView).should().visibility = View.VISIBLE
        then(valueTextView).should().setTextColor(R.color.tomato)
        then(valueTextView).shouldHaveNoMoreInteractions()

        inOrder(valueTextView).apply {
            verify(valueTextView).text = null
            verify(valueTextView).visibility = View.GONE
            verify(valueTextView).text = valueText
            verify(valueTextView).visibility = View.VISIBLE
        }

        then(infoTextView).should().text = null
        then(infoTextView).should().visibility = View.GONE
        then(valueTextView).shouldHaveNoMoreInteractions()

        then(targetImageView).should().setAddress(TEST_ADDRESS)
        then(targetImageView).shouldHaveNoMoreInteractions()

        then(targetTextView).should().setTextColor(R.color.blue)
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(timestampTextView).should().text = R.string.just_a_moment_ago.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()

        computationScheduler.advanceTimeBy(66, TimeUnit.SECONDS)

        then(timestampTextView).should(times(2)).text = R.string.just_a_moment_ago.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()

        then(targetTextView).should().text = "0x31...4523"
        then(targetTextView).should().text = "Test Name"
        then(targetTextView).should().visibility = View.VISIBLE
        then(targetTextView).should().setOnClickListener(MockUtils.any())
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(viewModel).should().observeTransactionStatus("id_1")
        then(viewModel).should().loadTransactionInfo("id_1")
        then(viewModel).should().loadTokenInfo(TEST_TOKEN.address)
        then(viewModel).shouldHaveNoMoreInteractions()

        then(transactionSubject).shouldHaveZeroInteractions()
        rxClickListener!!.onClick(itemView)
        then(transactionSubject).should().onNext("id_1")
        then(transactionSubject).shouldHaveNoMoreInteractions()
    }

    @Test
    fun bindTransactionEntryAssetTransfer() {
        testBindTransactionEntryAssetTransfer(Single.just(TEST_TOKEN), "- 1 TT")
    }

    @Test
    fun bindTransactionEntryAssetTransferUnknownToken() {
        testBindTransactionEntryAssetTransfer(Single.error(TimeoutException()), "- 1000000")
    }

    @Test
    fun bindTransactionEntryGenericTransaction() {
        context.mockGetString()
        context.mockGetStringWithArgs()
        context.mockGetColor()
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_timestamp, timestampTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_label, targetTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_type_icon, typeIconImageView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_value, valueTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_info, infoTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_image, targetImageView)

        given(viewModel.loadTransactionInfo(anyString())).willReturn(
            Single.just(
                TransactionInfo(
                    "id_1",
                    "chain_hash",
                    TEST_SAFE,
                    TransactionData.Generic(TEST_ADDRESS, BigInteger("100000000000000000"), "0x12345678"),
                    TEST_TIMESTAMP,
                    TEST_GAS_LIMIT,
                    TEST_GAS_PRICE,
                    ERC20Token.ETHER_TOKEN.address
                )
            )
        )
        given(viewModel.observeTransactionStatus(anyString())).willReturn(Observable.empty())
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("id_1"), emptyList())

        then(timestampTextView).should().text = R.string.loading.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(targetTextView).shouldHaveZeroInteractions()

        var rxClickListener: View.OnClickListener? = null
        given(itemView.setOnClickListener(MockUtils.any())).will {
            rxClickListener = it.arguments.first() as View.OnClickListener
            Unit
        }

        viewHolder.start()

        assertNotNull(rxClickListener)

        then(typeIconImageView).should().setImageResource(R.drawable.ic_transaction_outgoing)
        then(typeIconImageView).shouldHaveNoMoreInteractions()

        then(valueTextView).should().text = "- ${R.string.x_ether}, 0.1"
        then(valueTextView).should().visibility = View.VISIBLE
        then(valueTextView).should().setTextColor(R.color.tomato)
        then(valueTextView).shouldHaveNoMoreInteractions()

        then(infoTextView).should().text = "${R.string.x_data_bytes}, 4"
        then(infoTextView).should().visibility = View.VISIBLE
        then(valueTextView).shouldHaveNoMoreInteractions()

        then(targetImageView).should().setAddress(TEST_ADDRESS)
        then(targetImageView).shouldHaveNoMoreInteractions()

        then(targetTextView).should().setTextColor(R.color.blue)
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(timestampTextView).should().text = R.string.just_a_moment_ago.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()

        computationScheduler.advanceTimeBy(66, TimeUnit.SECONDS)

        then(timestampTextView).should(times(2)).text = R.string.just_a_moment_ago.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()

        then(targetTextView).should().text = "0x31...4523"
        then(targetTextView).should().setOnClickListener(MockUtils.any())
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(viewModel).should().observeTransactionStatus("id_1")
        then(viewModel).should().loadTransactionInfo("id_1")
        then(viewModel).shouldHaveNoMoreInteractions()

        then(transactionSubject).shouldHaveZeroInteractions()
        rxClickListener!!.onClick(itemView)
        then(transactionSubject).should().onNext("id_1")
        then(transactionSubject).shouldHaveNoMoreInteractions()
    }

    @Test
    fun bindTransactionEntryDetailsError() {
        context.mockGetString()
        context.mockGetColor()
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_timestamp, timestampTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_label, targetTextView)

        given(viewModel.loadTransactionInfo(anyString())).willReturn(Single.error(NoSuchElementException()))
        given(viewModel.observeTransactionStatus(anyString())).willReturn(Observable.empty())

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("id_1"), emptyList())

        then(timestampTextView).should().text = R.string.loading.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()

        viewHolder.start()

        then(targetTextView).should().setTextColor(R.color.blue)
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(timestampTextView).should().text = R.string.transaction_details_error.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()
    }

    @Test
    fun bindOtherEntry() {
        context.mockGetString()
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_timestamp, timestampTextView)

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Header(SUBMITTED), emptyList())

        then(timestampTextView).should().text = R.string.loading.toString()

        viewHolder.start()

        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(viewModel).shouldHaveZeroInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun updateStatus() {
        context.mockGetString()
        context.mockGetStringWithArgs()
        context.mockGetColor()
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_timestamp, timestampTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_label, targetTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_type_image, typeImageView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_type_icon, typeIconImageView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_value, valueTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_info, infoTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_image, targetImageView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_progress, progressView)

        val infoSingleFactory = TestSingleFactory<TransactionInfo>()
        given(viewModel.loadTransactionInfo(anyString())).willReturn(infoSingleFactory.get())

        val statusSubject = PublishSubject.create<TransactionExecutionRepository.PublishStatus>()
        given(viewModel.observeTransactionStatus(anyString())).willReturn(statusSubject)

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("id_1"), emptyList())

        then(timestampTextView).should().text = R.string.loading.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(targetTextView).shouldHaveZeroInteractions()

        viewHolder.start()

        then(targetTextView).should().setTextColor(R.color.blue)
        then(targetTextView).shouldHaveNoMoreInteractions()

        statusSubject.onNext(TransactionExecutionRepository.PublishStatus.Unknown)
        then(progressView).should().isIndeterminate = false
        then(progressView).should().max = 100
        then(progressView).should().progress = 0
        then(typeImageView).should().visibility = View.GONE
        then(targetImageView).should().visibility = View.VISIBLE
        then(progressView).shouldHaveNoMoreInteractions()
        then(typeImageView).shouldHaveNoMoreInteractions()
        then(targetImageView).shouldHaveNoMoreInteractions()

        statusSubject.onNext(TransactionExecutionRepository.PublishStatus.Success(TEST_TIMESTAMP))
        then(progressView).should(times(2)).isIndeterminate = false
        then(progressView).should(times(2)).max = 100
        then(progressView).should(times(2)).progress = 0
        then(typeImageView).should(times(2)).visibility = View.GONE
        then(targetImageView).should(times(2)).visibility = View.VISIBLE
        then(progressView).shouldHaveNoMoreInteractions()
        then(typeImageView).shouldHaveNoMoreInteractions()
        then(targetImageView).shouldHaveNoMoreInteractions()

        statusSubject.onNext(TransactionExecutionRepository.PublishStatus.Pending)
        then(progressView).should(times(3)).isIndeterminate = false
        then(progressView).should().isIndeterminate = true
        then(progressView).should(times(3)).max = 100
        then(progressView).should(times(3)).progress = 0
        then(typeImageView).should(times(3)).visibility = View.GONE
        then(targetImageView).should(times(3)).visibility = View.VISIBLE
        then(progressView).shouldHaveNoMoreInteractions()
        then(typeImageView).shouldHaveNoMoreInteractions()
        then(targetImageView).shouldHaveNoMoreInteractions()

        statusSubject.onNext(TransactionExecutionRepository.PublishStatus.Failed(TEST_TIMESTAMP))
        then(progressView).should(times(4)).isIndeterminate = false
        then(progressView).should(times(4)).max = 100
        then(progressView).should(times(4)).progress = 0
        then(typeImageView).should().visibility = View.VISIBLE
        then(typeImageView).should().setImageResource(R.drawable.ic_transaction_failed)
        then(targetImageView).should().visibility = View.INVISIBLE
        then(valueTextView).should().setTextColor(R.color.dark_grey)
        then(targetTextView).should().setTextColor(R.color.tomato)

        then(progressView).shouldHaveNoMoreInteractions()
        then(typeImageView).shouldHaveNoMoreInteractions()
        then(typeIconImageView).shouldHaveNoMoreInteractions()
        then(valueTextView).shouldHaveNoMoreInteractions()
        then(valueTextView).shouldHaveNoMoreInteractions()
        then(targetImageView).shouldHaveNoMoreInteractions()
        then(targetTextView).shouldHaveNoMoreInteractions()
        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(viewModel).should().observeTransactionStatus("id_1")
        then(viewModel).should().loadTransactionInfo("id_1")
        then(viewModel).shouldHaveNoMoreInteractions()
    }

    private fun testSettingsEntry(entry: TransactionData, text: String) {
        context.mockGetString()
        context.mockGetColor()
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_timestamp, timestampTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_label, targetTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_type_icon, typeIconImageView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_value, valueTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_info, infoTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_image, targetImageView)

        given(viewModel.loadTransactionInfo(anyString())).willReturn(
            Single.just(
                TransactionInfo(
                    "id_1",
                    "chain_hash",
                    TEST_SAFE,
                    entry,
                    TEST_TIMESTAMP,
                    TEST_GAS_LIMIT,
                    TEST_GAS_PRICE,
                    ERC20Token.ETHER_TOKEN.address
                )
            )
        )
        given(viewModel.observeTransactionStatus(anyString())).willReturn(Observable.empty())
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_SAFE, "Test Safe", "")))

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("id_1"), emptyList())

        then(timestampTextView).should().text = R.string.loading.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(targetTextView).shouldHaveZeroInteractions()

        var rxClickListener: View.OnClickListener? = null
        given(itemView.setOnClickListener(MockUtils.any())).will {
            rxClickListener = it.arguments.first() as View.OnClickListener
            Unit
        }

        viewHolder.start()

        assertNotNull(rxClickListener)

        then(typeIconImageView).should().setImageResource(R.drawable.ic_transaction_settings)
        then(typeIconImageView).shouldHaveNoMoreInteractions()

        then(valueTextView).should().text = null
        then(valueTextView).should().visibility = View.GONE
        then(valueTextView).should().setTextColor(R.color.blue)
        then(valueTextView).shouldHaveNoMoreInteractions()

        then(infoTextView).should().text = text
        then(infoTextView).should().visibility = View.VISIBLE
        then(infoTextView).shouldHaveNoMoreInteractions()

        then(targetImageView).should().setAddress(TEST_SAFE)
        then(targetImageView).shouldHaveNoMoreInteractions()

        then(targetTextView).should().setTextColor(R.color.blue)
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(timestampTextView).should().text = R.string.just_a_moment_ago.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()

        computationScheduler.advanceTimeBy(66, TimeUnit.SECONDS)

        then(timestampTextView).should(times(2)).text = R.string.just_a_moment_ago.toString()
        then(timestampTextView).shouldHaveNoMoreInteractions()

        then(targetTextView).should().text = "0xA7...46EC"
        then(targetTextView).should().text = "Test Safe"
        then(targetTextView).should().visibility = View.VISIBLE
        then(targetTextView).should().setOnClickListener(MockUtils.any())
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(viewModel).should().observeTransactionStatus("id_1")
        then(viewModel).should().loadTransactionInfo("id_1")
        then(viewModel).shouldHaveNoMoreInteractions()

        then(transactionSubject).shouldHaveZeroInteractions()
        rxClickListener!!.onClick(itemView)
        then(transactionSubject).should().onNext("id_1")
        then(transactionSubject).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testBindTransactionEntryReplaceRecoveryPhrase() {
        testSettingsEntry(TransactionData.ReplaceRecoveryPhrase(
            SafeTransaction(
                Transaction(
                    address = Solidity.Address(100.toBigInteger())
                ), operation = TransactionExecutionRepository.Operation.DELEGATE_CALL
            )
        ), R.string.replaced_recovery_phrase.toString())
    }

    @Test
    fun testBindTransactionEntryUpdateMasterCopy() {
        testSettingsEntry(TransactionData.UpdateMasterCopy(TEST_ADDRESS), R.string.contract_upgrade.toString())
    }

    @Test
    fun testBindTransactionEntryConnectExtension() {
        testSettingsEntry(TransactionData.ConnectAuthenticator(TEST_ADDRESS), R.string.connect_authenticator.toString())
    }

    @Test
    fun stop() {
        context.mockGetString()
        context.mockGetColor()
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_timestamp, timestampTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_label, targetTextView)

        val infoSingleFactory = TestSingleFactory<TransactionInfo>()
        given(viewModel.loadTransactionInfo(anyString())).willReturn(infoSingleFactory.get())

        val statusObservableFactory = TestObservableFactory<TransactionExecutionRepository.PublishStatus>()
        given(viewModel.observeTransactionStatus(anyString())).willReturn(statusObservableFactory.get())

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("id_1"), emptyList())

        then(timestampTextView).should().text = R.string.loading.toString()

        viewHolder.start()

        then(targetTextView).should().setTextColor(R.color.blue)
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(viewModel).should().loadTransactionInfo("id_1")
        then(viewModel).should().observeTransactionStatus("id_1")
        then(viewModel).shouldHaveNoMoreInteractions()

        infoSingleFactory.assertAllSubscribed()
        statusObservableFactory.assertAllSubscribed()

        viewHolder.stop()

        infoSingleFactory.assertAllCanceled()
        statusObservableFactory.assertAllCanceled()

        then(viewModel).shouldHaveNoMoreInteractions()
        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
        then(safeRepository).shouldHaveZeroInteractions()

        then(itemView).should().setOnClickListener(null)
    }

    @Test
    fun unbind() {
        context.mockGetString()
        context.mockGetColor()
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_timestamp, timestampTextView)
        itemView.mockFindViewById(R.id.layout_safe_transactions_item_target_label, targetTextView)

        val infoSingleFactory = TestSingleFactory<TransactionInfo>()
        given(viewModel.loadTransactionInfo(anyString())).willReturn(infoSingleFactory.get())

        val statusObservableFactory = TestObservableFactory<TransactionExecutionRepository.PublishStatus>()
        given(viewModel.observeTransactionStatus(anyString())).willReturn(statusObservableFactory.get())

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("id_1"), emptyList())

        then(timestampTextView).should().text = R.string.loading.toString()

        viewHolder.start()

        then(targetTextView).should().setTextColor(R.color.blue)
        then(targetTextView).shouldHaveNoMoreInteractions()

        then(viewModel).should().loadTransactionInfo("id_1")
        then(viewModel).should().observeTransactionStatus("id_1")
        then(viewModel).shouldHaveNoMoreInteractions()

        infoSingleFactory.assertAllSubscribed()
        statusObservableFactory.assertAllSubscribed()

        viewHolder.unbind()

        infoSingleFactory.assertAllCanceled()
        statusObservableFactory.assertAllCanceled()

        // Nothing should happening without a new bind
        viewHolder.start()

        then(targetTextView).shouldHaveNoMoreInteractions()
        then(viewModel).shouldHaveNoMoreInteractions()
        then(timestampTextView).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
        then(safeRepository).shouldHaveZeroInteractions()

        then(itemView).should().setOnClickListener(null)
    }

    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private val TEST_ADDRESS = "0x31B98D14007bDEe637298086988A0bBd31184523".asEthereumAddress()!!
        private val TEST_TIMESTAMP = System.currentTimeMillis()
        private val TEST_TOKEN = ERC20Token(
            "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!,
            name = "Test Token",
            symbol = "TT",
            decimals = 6
        )
        private val TEST_GAS_LIMIT = BigInteger.valueOf(100000)
        private val TEST_GAS_PRICE = BigInteger.valueOf(20000000000)
    }
}
