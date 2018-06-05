package pm.gnosis.heimdall.ui.transactions.review.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.observers.TestObserver
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
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.builder.GenericTransactionBuilder
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress

@RunWith(MockitoJUnitRunner::class)
class GenericTransactionViewHolderTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    @Mock
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    private lateinit var containerView: ViewGroup

    @Mock
    private lateinit var viewHolderView: View

    @Mock
    private lateinit var valueView: TextView

    @Mock
    private lateinit var dataView: TextView

    @Mock
    private lateinit var safeAddressView: TextView

    @Mock
    private lateinit var safeNameView: TextView

    @Mock
    private lateinit var safeImageView: BlockiesImageView

    @Mock
    private lateinit var receiverAddressView: TextView

    @Mock
    private lateinit var receiverNameView: TextView

    @Mock
    private lateinit var receiverImageView: BlockiesImageView

    @Mock
    private lateinit var contextMock: Context

    private lateinit var addressHelper: AddressHelper

    private lateinit var viewHolder: GenericTransactionViewHolder

    @Before
    fun setUp() {
        addressHelper = AddressHelper(addressBookRepository, safeRepository)
    }

    @Test
    fun testNoInteractionWithoutView() {
        val data = TransactionData.Generic(TEST_RECEIVER, TEST_VALUE, TEST_DATA)
        viewHolder = GenericTransactionViewHolder(TEST_SAFE, data, addressHelper)
        viewHolder.start()
        then(contextMock).shouldHaveZeroInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
        then(layoutInflater).shouldHaveZeroInteractions()
    }

    private fun setupViewMocks() {
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))
        given(layoutInflater.inflate(R.layout.layout_generic_transaction_info, containerView, true)).willReturn(viewHolderView)
        viewHolderView.mockFindViewById(R.id.layout_generic_transaction_info_value, valueView)
        viewHolderView.mockFindViewById(R.id.layout_generic_transaction_info_data, dataView)
        viewHolderView.mockFindViewById(R.id.layout_generic_transaction_info_safe_address, safeAddressView)
        viewHolderView.mockFindViewById(R.id.layout_generic_transaction_info_safe_name, safeNameView)
        viewHolderView.mockFindViewById(R.id.layout_generic_transaction_info_safe_image, safeImageView)
        viewHolderView.mockFindViewById(R.id.layout_generic_transaction_info_to_address, receiverAddressView)
        viewHolderView.mockFindViewById(R.id.layout_generic_transaction_info_to_name, receiverNameView)
        viewHolderView.mockFindViewById(R.id.layout_generic_transaction_info_to_image, receiverImageView)
    }

    private fun assertAddressHelperInteractions() {
        then(safeNameView).should().visibility = View.GONE
        then(safeNameView).shouldHaveNoMoreInteractions()
        then(safeImageView).should().setAddress(TEST_SAFE)
        then(safeImageView).shouldHaveNoMoreInteractions()
        then(safeAddressView).should().text = "0xFe21...5016ee"
        then(safeAddressView).shouldHaveNoMoreInteractions()

        then(receiverNameView).should().visibility = View.GONE
        then(receiverNameView).shouldHaveNoMoreInteractions()
        then(receiverImageView).should().setAddress(TEST_RECEIVER)
        then(receiverImageView).shouldHaveNoMoreInteractions()
        then(receiverAddressView).should().text = "0x0029...0CBAA2"
        then(receiverAddressView).shouldHaveNoMoreInteractions()

        then(addressBookRepository).should().loadAddressBookEntry(TEST_RECEIVER)
        then(addressBookRepository).should().loadAddressBookEntry(TEST_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).should().loadSafe(TEST_RECEIVER)
        then(safeRepository).should().loadSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testWithData() {
        setupViewMocks()
        given(viewHolderView.context).willReturn(contextMock)
        contextMock.mockGetStringWithArgs()

        val data = TransactionData.Generic(TEST_RECEIVER, TEST_VALUE, TEST_DATA)
        viewHolder = GenericTransactionViewHolder(TEST_SAFE, data, addressHelper)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_generic_transaction_info, containerView, true)

        viewHolder.start()
        then(valueView).should().text = "${R.string.x_ether}, 3.141"
        then(dataView).should().text = "${R.string.x_data_bytes}, 26"

        // Check view interaction
        assertAddressHelperInteractions()

        then(contextMock).should().getString(R.string.x_ether, "3.141")
        then(contextMock).should().getString(R.string.x_data_bytes, 26)
        then(contextMock).shouldHaveZeroInteractions()

    }

    @Test
    fun testWithoutData() {
        setupViewMocks()
        given(viewHolderView.context).willReturn(contextMock)
        contextMock.mockGetStringWithArgs()

        val data = TransactionData.Generic(TEST_RECEIVER, TEST_VALUE, null)
        viewHolder = GenericTransactionViewHolder(TEST_SAFE, data, addressHelper)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_generic_transaction_info, containerView, true)

        viewHolder.start()
        then(valueView).should().text = "${R.string.x_ether}, 3.141"
        then(dataView).should().text = "${R.string.x_data_bytes}, 0"

        // Check view interaction
        assertAddressHelperInteractions()

        then(contextMock).should().getString(R.string.x_ether, "3.141")
        then(contextMock).should().getString(R.string.x_data_bytes, 0)
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun testStop() {
        given(viewHolderView.context).willReturn(contextMock)
        contextMock.mockGetStringWithArgs()
        setupViewMocks()
        val safeObservable = TestSingleFactory<Safe>()
        given(safeRepository.loadSafe(TEST_SAFE)).willReturn(safeObservable.get())

        val data = TransactionData.Generic(TEST_RECEIVER, TEST_VALUE, TEST_DATA)
        viewHolder = GenericTransactionViewHolder(TEST_SAFE, data, addressHelper)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_generic_transaction_info, containerView, true)

        viewHolder.start()
        safeObservable.assertAllSubscribed()

        viewHolder.stop()
        safeObservable.assertAllCanceled()
    }

    @Test
    fun testDetach() {
        given(viewHolderView.context).willReturn(contextMock)
        contextMock.mockGetStringWithArgs()
        setupViewMocks()
        val safeObservable = TestSingleFactory<Safe>()
        given(safeRepository.loadSafe(TEST_SAFE)).willReturn(safeObservable.get())

        val data = TransactionData.Generic(TEST_RECEIVER, TEST_VALUE, TEST_DATA)
        viewHolder = GenericTransactionViewHolder(TEST_SAFE, data, addressHelper)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_generic_transaction_info, containerView, true)

        viewHolder.start()
        safeObservable.assertAllSubscribed()

        viewHolder.detach()
        safeObservable.assertAllCanceled()

        then(addressBookRepository).should().loadAddressBookEntry(TEST_SAFE)
        then(addressBookRepository).should().loadAddressBookEntry(TEST_RECEIVER)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).should().loadSafe(TEST_SAFE)
        then(safeRepository).should().loadSafe(TEST_RECEIVER)
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(layoutInflater).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.x_ether, "3.141")
        then(contextMock).should().getString(R.string.x_data_bytes, 26)
        then(contextMock).shouldHaveNoMoreInteractions()

        // Check that it is detached from the view
        viewHolder.start()
        then(contextMock).shouldHaveZeroInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
        then(layoutInflater).shouldHaveZeroInteractions()
    }

    @Test
    fun testLoadTransaction() {
        val data = TransactionData.Generic(TEST_RECEIVER, TEST_VALUE, TEST_DATA)
        viewHolder = GenericTransactionViewHolder(TEST_SAFE, data, addressHelper)
        val testObserver = TestObserver<SafeTransaction>()
        viewHolder.loadTransaction().subscribe(testObserver)
        testObserver.assertResult(GenericTransactionBuilder.build(data))
    }

    companion object {
        private val TEST_SAFE = "Fe2149773B3513703E79Ad23D05A778A185016ee".asEthereumAddress()!!
        private val TEST_VALUE = Wei.ether("3.141").value
        private val TEST_RECEIVER = "0029840843f03C164c29A78114b300Ba9E0CBAA2".asEthereumAddress()!!
        private const val TEST_DATA = "0x146345791248658456839129424372923958afafafafafa00000"
    }

}
