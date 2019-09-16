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
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestSingleFactory
import pm.gnosis.tests.utils.mockFindViewById
import pm.gnosis.utils.asEthereumAddress

@RunWith(MockitoJUnitRunner::class)
class SafeAdapterViewHolderTest {
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

    @Mock
    private lateinit var safeImageView: BlockiesImageView

    private var rxClickListener: View.OnClickListener? = null

    private lateinit var addressHelper: AddressHelper

    private lateinit var viewHolder: SafeAdapter.ViewHolder

    @Before
    fun setUp() {
        given(itemView.setOnClickListener(MockUtils.any())).will {
            rxClickListener = it.arguments.first() as View.OnClickListener
            Unit
        }
        addressHelper = AddressHelper(addressBookRepository)
        viewHolder = SafeAdapter.ViewHolder(itemView, safeSubject, addressHelper)
    }

    @Test
    fun bindWrongType() {
        val safeObserver = TestObserver<AbstractSafe>()
        safeSubject.subscribe(safeObserver)
        viewHolder.bind(PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT), emptyList())
        viewHolder.start()

        then(itemView).should().setOnClickListener(viewHolder)
        then(itemView).shouldHaveZeroInteractions()

        assertNotNull(rxClickListener)
        rxClickListener?.onClick(itemView)
        safeObserver.assertEmpty()
    }

    @Test
    fun bindSafeAndSelect() {
        val safeObserver = TestObserver<AbstractSafe>()
        safeSubject.subscribe(safeObserver)
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_SAFE, "Some other name!", "")))
        then(itemView).should().setOnClickListener(viewHolder)
        itemView.mockFindViewById(R.id.layout_safe_item_address, safeAddressTextView)
        itemView.mockFindViewById(R.id.layout_safe_item_name, safeNameTextView)
        itemView.mockFindViewById(R.id.layout_safe_item_image, safeImageView)

        val safe = Safe(TEST_SAFE)
        viewHolder.bind(safe, emptyList())

        then(safeAddressTextView).should().text = null
        then(safeAddressTextView).shouldHaveNoMoreInteractions()

        then(safeNameTextView).should().text = null
        then(safeNameTextView).shouldHaveNoMoreInteractions()

        viewHolder.start()

        then(safeAddressTextView).should().text = "0x1f...5C7E"
        then(safeAddressTextView).should().setOnClickListener(MockUtils.any())
        then(safeAddressTextView).shouldHaveNoMoreInteractions()

        then(safeNameTextView).should().text = "Some other name!"
        then(safeNameTextView).should().visibility = View.VISIBLE
        then(safeNameTextView).shouldHaveNoMoreInteractions()

        then(safeImageView).should().setAddress(TEST_SAFE)
        then(safeImageView).shouldHaveNoMoreInteractions()

        then(addressBookRepository).should().loadAddressBookEntry(TEST_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()

        assertNotNull(rxClickListener)
        rxClickListener?.onClick(itemView)
        safeObserver.assertValues(safe)
    }

    @Test
    fun stop() {
        itemView.mockFindViewById(R.id.layout_safe_item_address, safeAddressTextView)
        itemView.mockFindViewById(R.id.layout_safe_item_name, safeNameTextView)
        itemView.mockFindViewById(R.id.layout_safe_item_image, safeImageView)
        val addressBookSingleFactory = TestSingleFactory<AddressBookEntry>()
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(addressBookSingleFactory.get())

        val safe = Safe(TEST_SAFE)
        viewHolder.bind(safe, emptyList())
        viewHolder.start()

        addressBookSingleFactory.assertAllSubscribed()

        viewHolder.stop()

        addressBookSingleFactory.assertAllCanceled()
    }

    @Test
    fun unbind() {
        itemView.mockFindViewById(R.id.layout_safe_item_address, safeAddressTextView)
        itemView.mockFindViewById(R.id.layout_safe_item_name, safeNameTextView)
        itemView.mockFindViewById(R.id.layout_safe_item_image, safeImageView)
        val addressBookSingleFactory = TestSingleFactory<AddressBookEntry>()
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(addressBookSingleFactory.get())

        val safe = Safe(TEST_SAFE)
        viewHolder.bind(safe, emptyList())
        viewHolder.start()

        then(safeAddressTextView).should().text = null
        then(safeAddressTextView).should().text = "0x1f...5C7E"
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
        private val TEST_PAYMENT_TOKEN = ERC20Token.ETHER_TOKEN.address
        private val TEST_PAYMENT_AMOUNT = Wei.ether("0.1").value
    }

}
