package pm.gnosis.heimdall.ui.transactions.view.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import io.reactivex.observers.TestObserver
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import java.util.*
import kotlin.random.Random

@RunWith(MockitoJUnitRunner::class)
class MultiSendViewHolderTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var addressHelperMock: AddressHelper

    @Mock
    private lateinit var layoutInflaterMock: LayoutInflater

    @Mock
    private lateinit var viewHolderViewMock: ViewGroup

    @Mock
    private lateinit var containerViewMock: ViewGroup

    @Mock
    private lateinit var addressViewMock: TextView

    @Mock
    private lateinit var nameViewMock: TextView

    @Mock
    private lateinit var descriptionViewMock: TextView

    @Mock
    private lateinit var detailsBtnViewMock: TextView

    @Mock
    private lateinit var imageViewMock: BlockiesImageView

    @Captor
    private lateinit var clickListenerArgumentCaptor: ArgumentCaptor<View.OnClickListener>

    private lateinit var viewHolder: MultiSendViewHolder

    @Test
    fun loadTransaction() {
        viewHolder = MultiSendViewHolder(addressHelperMock, TransactionData.MultiSend(emptyList(), MULTI_SEND), TEST_SAFE)
        val testObserver = TestObserver.create<SafeTransaction>()
        val expectedTransaction = SafeTransaction(
            Transaction(
                address = MULTI_SEND,
                data = MultiSend.MultiSend.encode(Solidity.Bytes(byteArrayOf()))
            ), operation = TransactionExecutionRepository.Operation.DELEGATE_CALL
        )

        viewHolder.loadTransaction().subscribe(testObserver)

        testObserver.assertResult(expectedTransaction)
    }

    @Test
    fun loadAssetChange() {
        viewHolder = MultiSendViewHolder(addressHelperMock, TransactionData.MultiSend(emptyList(), MULTI_SEND), TEST_SAFE)
        val testObserver = TestObserver.create<Optional<ERC20TokenWithBalance>>()
        viewHolder.loadAssetChange().subscribe(testObserver)
        testObserver.assertResult(None)
    }

    @Test
    fun testNoInteractionWithoutView() {
        viewHolder = MultiSendViewHolder(addressHelperMock, TransactionData.MultiSend(emptyList(), MULTI_SEND), TEST_SAFE)
        viewHolder.start()
        then(addressHelperMock).shouldHaveZeroInteractions()
    }

    @Test
    fun start() {
        given(viewHolderViewMock.context).willReturn(contextMock)
        contextMock.mockGetStringWithArgs()
        // Set view
        val transactionCount = Random.nextInt(10)
        val data = TransactionData.MultiSend((0..transactionCount).map {
            SafeTransaction(
                Transaction(TEST_SAFE),
                TransactionExecutionRepository.Operation.CALL
            )
        }, MULTI_SEND)
        viewHolder = MultiSendViewHolder(addressHelperMock, data, TEST_SAFE)
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_multi_send_transaction_info, containerViewMock, true)

        // start()
        mockViews()
        val singleFactory = TestSingleFactory<String>()
        given(
            addressHelperMock.buildAddressInfoSingle(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(singleFactory.get())

        viewHolder.start()

        then(imageViewMock).should().setAddress(TEST_SAFE)
        then(imageViewMock).shouldHaveNoMoreInteractions()
        then(addressHelperMock).should().buildAddressInfoSingle(addressViewMock, nameViewMock, TEST_SAFE)
        then(addressHelperMock).shouldHaveNoMoreInteractions()

        then(descriptionViewMock).should().text = "${R.string.this_will_perform_x_transactions}, ${transactionCount + 1}"
        then(descriptionViewMock).shouldHaveNoMoreInteractions()

        singleFactory.success("Some Safe Name")
        then(descriptionViewMock).shouldHaveNoMoreInteractions()

        then(detailsBtnViewMock).should().setOnClickListener(capture(clickListenerArgumentCaptor))

        clickListenerArgumentCaptor.value.onClick(detailsBtnViewMock)
        then(contextMock).should().startActivity(MockUtils.any())
    }

    @Test
    fun detach() {
        viewHolder = MultiSendViewHolder(addressHelperMock, TransactionData.MultiSend(emptyList(), MULTI_SEND), TEST_SAFE)
        given(viewHolderViewMock.context).willReturn(contextMock)
        // Set view
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_multi_send_transaction_info, containerViewMock, true)

        // start() - View should not be null and views are filled
        mockViews()
        val singleFactory = TestSingleFactory<String>()
        given(
            addressHelperMock.buildAddressInfoSingle(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(singleFactory.get())

        viewHolder.start()

        then(imageViewMock).should().setAddress(TEST_SAFE)
        then(imageViewMock).shouldHaveNoMoreInteractions()
        then(addressHelperMock).should().buildAddressInfoSingle(addressViewMock, nameViewMock, TEST_SAFE)
        then(addressHelperMock).shouldHaveNoMoreInteractions()

        // detach()
        viewHolder.detach()

        // start() - View is null. No action performed
        viewHolder.start()
        then(addressHelperMock).shouldHaveZeroInteractions()
    }

    @Test
    fun stop() {
        viewHolder = MultiSendViewHolder(addressHelperMock, TransactionData.MultiSend(emptyList(), MULTI_SEND), TEST_SAFE)
        given(viewHolderViewMock.context).willReturn(contextMock)
        // Set view
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_multi_send_transaction_info, containerViewMock, true)

        // start() - View should not be null and views are filled
        mockViews()
        val singleFactory = TestSingleFactory<String>()
        given(
            addressHelperMock.buildAddressInfoSingle(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(singleFactory.get())

        viewHolder.start()

        then(imageViewMock).should().setAddress(TEST_SAFE)
        then(imageViewMock).shouldHaveNoMoreInteractions()
        then(addressHelperMock).should().buildAddressInfoSingle(addressViewMock, nameViewMock, TEST_SAFE)
        then(addressHelperMock).shouldHaveNoMoreInteractions()

        // stop()
        viewHolder.stop()

        singleFactory.assertAllCanceled()
    }

    private fun mockViews() {
        viewHolderViewMock.mockFindViewById(R.id.multi_send_transaction_info_safe_address, addressViewMock)
        viewHolderViewMock.mockFindViewById(R.id.multi_send_transaction_info_safe_name, nameViewMock)
        viewHolderViewMock.mockFindViewById(R.id.multi_send_transaction_info_safe_image, imageViewMock)
        viewHolderViewMock.mockFindViewById(R.id.multi_send_transaction_info_description, descriptionViewMock)
        viewHolderViewMock.mockFindViewById(R.id.multi_send_transaction_info_details_btn, detailsBtnViewMock)
    }

    companion object {
        private val MULTI_SEND = BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!
        private val TEST_SAFE = "0x43".asEthereumAddress()!!
    }
}
