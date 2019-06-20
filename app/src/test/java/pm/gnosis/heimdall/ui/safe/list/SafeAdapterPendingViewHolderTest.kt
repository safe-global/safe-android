package pm.gnosis.heimdall.ui.safe.list

import android.view.View
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.*
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestSingleFactory
import pm.gnosis.tests.utils.mockFindViewById
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeAdapterPendingViewHolderTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private val safeSubject = PublishSubject.create<AbstractSafe>()

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var itemView: View

    @Mock
    private lateinit var safeNameTextView: TextView

    @Mock
    private lateinit var safeAddressTextView: TextView

    private var rxClickListener: View.OnClickListener? = null

    private lateinit var addressHelper: AddressHelper

    private lateinit var viewHolder: SafeAdapter.PendingViewHolder

    @Before
    fun setUp() {
        given(itemView.setOnClickListener(MockUtils.any())).will {
            rxClickListener = it.arguments.first() as View.OnClickListener
            Unit
        }
        addressHelper = AddressHelper(addressBookRepository)
        viewHolder = SafeAdapter.PendingViewHolder(itemView, safeSubject, addressHelper)
    }

    @Test
    fun bindWrongType() {
        val safeObserver = TestObserver<AbstractSafe>()
        safeSubject.subscribe(safeObserver)
        viewHolder.bind(Safe(TEST_SAFE), emptyList())
        viewHolder.start()

        then(itemView).should().setOnClickListener(viewHolder)
        then(itemView).shouldHaveZeroInteractions()

        assertNotNull(rxClickListener)
        rxClickListener?.onClick(itemView)
        safeObserver.assertEmpty()
    }

    private fun bindSafeAndSelect(safe: AbstractSafe, address: Solidity.Address, shortenedAddress: String) {
        val safeObserver = TestObserver<AbstractSafe>()
        safeSubject.subscribe(safeObserver)
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(address, "Some other name!", "")))
        then(itemView).should().setOnClickListener(viewHolder)
        itemView.mockFindViewById(R.id.layout_pending_safe_item_address, safeAddressTextView)
        itemView.mockFindViewById(R.id.layout_pending_safe_item_name, safeNameTextView)

        viewHolder.bind(safe, emptyList())

        then(safeAddressTextView).should().text = null
        then(safeAddressTextView).shouldHaveNoMoreInteractions()

        then(safeNameTextView).should().text = null
        then(safeNameTextView).shouldHaveNoMoreInteractions()

        viewHolder.start()

        then(safeAddressTextView).should().text = shortenedAddress
        then(safeAddressTextView).should().setOnClickListener(MockUtils.any())
        then(safeAddressTextView).shouldHaveNoMoreInteractions()

        then(safeNameTextView).should().text = "Some other name!"
        then(safeNameTextView).should().visibility = View.VISIBLE
        then(safeNameTextView).shouldHaveNoMoreInteractions()

        then(addressBookRepository).should().loadAddressBookEntry(address)
        then(addressBookRepository).shouldHaveNoMoreInteractions()

        assertNotNull(rxClickListener)
        rxClickListener?.onClick(itemView)
        safeObserver.assertValues(safe)
    }

    @Test
    fun bindPendingSafeAndSelect() {
        val safe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
        bindSafeAndSelect(safe, TEST_PENDING_SAFE, "0xC2...8132")
    }

    @Test
    fun bindRecoveringSafeAndSelect() {
        val safe = RecoveringSafe(
            TEST_RECOVERING_SAFE, BigInteger.ZERO, TEST_RECOVERING_SAFE, "", BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
            ERC20Token.ETHER_TOKEN.address, BigInteger.ZERO, BigInteger.ZERO, TransactionExecutionRepository.Operation.CALL, emptyList()
        )
        bindSafeAndSelect(safe, TEST_RECOVERING_SAFE, "0xb3...244A")
    }

    @Test
    fun stop() {
        itemView.mockFindViewById(R.id.layout_pending_safe_item_address, safeAddressTextView)
        itemView.mockFindViewById(R.id.layout_pending_safe_item_name, safeNameTextView)
        val addressBookSingleFactory = TestSingleFactory<AddressBookEntry>()
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(addressBookSingleFactory.get())

        val safe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
        viewHolder.bind(safe, emptyList())
        viewHolder.start()

        addressBookSingleFactory.assertAllSubscribed()

        viewHolder.stop()

        addressBookSingleFactory.assertAllCanceled()
    }

    @Test
    fun unbind() {
        itemView.mockFindViewById(R.id.layout_pending_safe_item_address, safeAddressTextView)
        itemView.mockFindViewById(R.id.layout_pending_safe_item_name, safeNameTextView)
        val addressBookSingleFactory = TestSingleFactory<AddressBookEntry>()
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(addressBookSingleFactory.get())

        val safe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
        viewHolder.bind(safe, emptyList())
        viewHolder.start()

        then(safeAddressTextView).should().text = null
        then(safeAddressTextView).should().text = "0xC2...8132"
        then(safeAddressTextView).should().setOnClickListener(MockUtils.any())
        then(safeAddressTextView).shouldHaveNoMoreInteractions()

        addressBookSingleFactory.assertAllSubscribed()

        viewHolder.unbind()

        addressBookSingleFactory.assertAllCanceled()

        viewHolder.start()
        // No entry bound, there should be no interaction
        then(safeAddressTextView).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_PENDING_SAFE = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_RECOVERING_SAFE = "0xb36574155395D41b92664e7A215103262a14244A".asEthereumAddress()!!
        private val TEST_PAYMENT_TOKEN = ERC20Token.ETHER_TOKEN.address
        private val TEST_PAYMENT_AMOUNT = Wei.ether("0.1").value
    }

}
