package pm.gnosis.heimdall.ui.transactions.view.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class UpdateMasterCopyViewHolderTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var addressHelperMock: AddressHelper

    @Mock
    private lateinit var safeRepositoryMock: GnosisSafeRepository

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
    private lateinit var imageViewMock: BlockiesImageView

    private lateinit var viewHolder: UpdateMasterCopyViewHolder

    @Before
    fun setUp() {
        viewHolder = UpdateMasterCopyViewHolder(addressHelperMock, TransactionData.UpdateMasterCopy(TEST_MASTER_COPY), TEST_SAFE)
    }

    @Test
    fun loadTransaction() {
        val testObserver = TestObserver.create<SafeTransaction>()
        val expectedTransaction = SafeTransaction(
            Transaction(
                address = SAFE_INFO.address,
                data = GnosisSafe.ChangeMasterCopy.encode(TEST_MASTER_COPY)
            ), operation = TransactionExecutionRepository.Operation.CALL
        )

        viewHolder.loadTransaction().subscribe(testObserver)

        testObserver.assertResult(expectedTransaction)
    }

    @Test
    fun testNoInteractionWithoutView() {
        viewHolder.start()
        then(addressHelperMock).shouldHaveZeroInteractions()
    }

    @Test
    fun start() {
        given(viewHolderViewMock.context).willReturn(contextMock)
        contextMock.mockGetString()
        contextMock.mockGetStringWithArgs()
        // Set view
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_update_master_copy_transaction_info, containerViewMock, true)

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

        then(descriptionViewMock).should().text = "${R.string.this_will_upgrade}, ${R.string.default_safe_name}"
        then(descriptionViewMock).shouldHaveNoMoreInteractions()

        singleFactory.success("Some Safe Name")
        then(descriptionViewMock).should().text = "${R.string.this_will_upgrade}, Some Safe Name"
        then(descriptionViewMock).shouldHaveNoMoreInteractions()

    }

    @Test
    fun detach() {
        given(viewHolderViewMock.context).willReturn(contextMock)
        contextMock.mockGetString()
        // Set view
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_update_master_copy_transaction_info, containerViewMock, true)

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
        given(viewHolderViewMock.context).willReturn(contextMock)
        contextMock.mockGetString()
        // Set view
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_update_master_copy_transaction_info, containerViewMock, true)

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
        viewHolderViewMock.mockFindViewById(R.id.update_master_copy_transaction_info_safe_address, addressViewMock)
        viewHolderViewMock.mockFindViewById(R.id.update_master_copy_transaction_info_safe_name, nameViewMock)
        viewHolderViewMock.mockFindViewById(R.id.update_master_copy_transaction_info_safe_image, imageViewMock)
        viewHolderViewMock.mockFindViewById(R.id.update_master_copy_transaction_info_description, descriptionViewMock)
    }

    companion object {
        private val TEST_PHONE = "0x41".asEthereumAddress()!!
        private val TEST_MASTER_COPY = "0x42".asEthereumAddress()!!
        private val TEST_SAFE = "0x43".asEthereumAddress()!!
        private val SAFE_INFO = SafeInfo(
            address = TEST_SAFE,
            balance = Wei.ZERO,
            requiredConfirmations = 1,
            owners = listOf(TEST_PHONE),
            isOwner = true,
            modules = emptyList(),
            version = SemVer(1, 0, 0)
        )
    }
}
