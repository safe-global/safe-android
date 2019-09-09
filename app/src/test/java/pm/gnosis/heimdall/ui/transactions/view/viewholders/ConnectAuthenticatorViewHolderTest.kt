package pm.gnosis.heimdall.ui.transactions.view.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
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
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockFindViewById
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class ConnectAuthenticatorViewHolderTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

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
    private lateinit var imageViewMock: BlockiesImageView

    private lateinit var viewHolder: ConnectAuthenticatorViewHolder

    @Before
    fun setUp() {
        viewHolder = ConnectAuthenticatorViewHolder(addressHelperMock, TEST_EXTENSION, TEST_SAFE, safeRepositoryMock)
    }

    @Test
    fun loadTransaction() {
        val testObserver = TestObserver.create<SafeTransaction>()
        val expectedTransaction = SafeTransaction(
            Transaction(
                address = SAFE_INFO.address,
                data = GnosisSafe.AddOwnerWithThreshold.encode(
                    owner = TEST_EXTENSION,
                    _threshold = Solidity.UInt256(SAFE_INFO.requiredConfirmations.toBigInteger() + BigInteger.ONE)
                )
            ), operation = TransactionExecutionRepository.Operation.CALL
        )
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))

        viewHolder.loadTransaction().subscribe(testObserver)

        testObserver.assertResult(expectedTransaction)
    }

    @Test
    fun loadTransactionError() {
        val testObserver = TestObserver.create<SafeTransaction>()
        val exception = IllegalStateException()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewHolder.loadTransaction().subscribe(testObserver)

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun testNoInteractionWithoutView() {
        viewHolder.start()
        then(addressHelperMock).shouldHaveZeroInteractions()
    }

    @Test
    fun start() {
        // Set view
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_connect_extension_transaction_info, containerViewMock, true)

        // start()
        mockViews()
        val disposable = Mockito.mock(Disposable::class.java)
        given(
            addressHelperMock.populateAddressInfo(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(listOf(disposable))

        viewHolder.start()

        then(addressHelperMock).should().populateAddressInfo(addressViewMock, nameViewMock, imageViewMock, TEST_SAFE)
        then(addressHelperMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun detach() {
        // Set view
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_connect_extension_transaction_info, containerViewMock, true)

        // start() - View should not be null and views are filled
        val disposable = Mockito.mock(Disposable::class.java)
        mockViews()
        given(
            addressHelperMock.populateAddressInfo(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(listOf(disposable))

        viewHolder.start()

        then(addressHelperMock).should().populateAddressInfo(addressViewMock, nameViewMock, imageViewMock, TEST_SAFE)
        then(addressHelperMock).shouldHaveNoMoreInteractions()

        // detach()
        viewHolder.detach()

        // start() - View is null. No action performed
        viewHolder.start()
        then(addressHelperMock).shouldHaveZeroInteractions()
    }

    @Test
    fun stop() {
        // Set view
        given(layoutInflaterMock.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(layoutInflaterMock, containerViewMock)
        then(layoutInflaterMock).should().inflate(R.layout.layout_connect_extension_transaction_info, containerViewMock, true)

        // start() - View should not be null and views are filled
        val disposable = Mockito.mock(Disposable::class.java)
        mockViews()
        given(
            addressHelperMock.populateAddressInfo(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(listOf(disposable))

        viewHolder.start()

        then(addressHelperMock).should().populateAddressInfo(addressViewMock, nameViewMock, imageViewMock, TEST_SAFE)
        then(addressHelperMock).shouldHaveNoMoreInteractions()

        // stop()
        viewHolder.stop()
        then(disposable).should().dispose()
        then(disposable).shouldHaveNoMoreInteractions()
    }

    private fun mockViews() {
        viewHolderViewMock.mockFindViewById(R.id.layout_connect_extension_transaction_info_safe_address, addressViewMock)
        viewHolderViewMock.mockFindViewById(R.id.layout_connect_extension_transaction_info_safe_name, nameViewMock)
        viewHolderViewMock.mockFindViewById(R.id.layout_connect_extension_transaction_info_safe_image, imageViewMock)
    }

    companion object {
        private val TEST_PHONE = "0x41".asEthereumAddress()!!
        private val TEST_EXTENSION = "0x42".asEthereumAddress()!!
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
