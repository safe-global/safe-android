package pm.gnosis.heimdall.ui.transactions.view.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockFindViewById

@RunWith(MockitoJUnitRunner::class)
class ReplaceRecoveryPhraseViewHolderTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var addressHelperMock: AddressHelper

    @Mock
    private lateinit var inflater: LayoutInflater

    @Mock
    private lateinit var containerViewMock: ViewGroup

    @Mock
    private lateinit var viewHolderViewMock: View

    @Mock
    private lateinit var addressViewMock: TextView

    @Mock
    private lateinit var nameViewMock: TextView

    @Mock
    private lateinit var imageViewMock: BlockiesImageView

    private lateinit var viewHolder: ReplaceRecoveryPhraseViewHolder

    @Before
    fun setup() {
        viewHolder = ReplaceRecoveryPhraseViewHolder(addressHelperMock, SAFE_ADDRESS, SAFE_TRANSACTION)
    }

    @Test
    fun testNoInteractionWithoutView() {
        viewHolder.start()
        then(addressHelperMock).shouldHaveZeroInteractions()
    }

    @Test
    fun start() {
        // Set view
        given(inflater.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(inflater, containerViewMock)
        then(inflater).should().inflate(R.layout.layout_replace_recovery_phrase_transaction_info, containerViewMock, true)

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

        then(addressHelperMock).should().populateAddressInfo(addressViewMock, nameViewMock, imageViewMock, SAFE_ADDRESS)
        then(addressHelperMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransaction() {
        val testObserver = TestObserver.create<SafeTransaction>()

        viewHolder.loadTransaction().subscribe(testObserver)

        testObserver.assertResult(SAFE_TRANSACTION)
    }

    @Test
    fun detach() {
        // Set view
        given(inflater.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(inflater, containerViewMock)
        then(inflater).should().inflate(R.layout.layout_replace_recovery_phrase_transaction_info, containerViewMock, true)

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

        then(addressHelperMock).should().populateAddressInfo(addressViewMock, nameViewMock, imageViewMock, SAFE_ADDRESS)
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
        given(inflater.inflate(anyInt(), MockUtils.any(), anyBoolean())).willReturn(viewHolderViewMock)
        viewHolder.inflate(inflater, containerViewMock)
        then(inflater).should().inflate(R.layout.layout_replace_recovery_phrase_transaction_info, containerViewMock, true)

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

        then(addressHelperMock).should().populateAddressInfo(addressViewMock, nameViewMock, imageViewMock, SAFE_ADDRESS)
        then(addressHelperMock).shouldHaveNoMoreInteractions()

        // stop()
        viewHolder.stop()
        then(disposable).should().dispose()
        then(disposable).shouldHaveNoMoreInteractions()
    }

    private fun mockViews() {
        viewHolderViewMock.mockFindViewById(R.id.layout_replace_recovery_phrase_transaction_info_safe_address, addressViewMock)
        viewHolderViewMock.mockFindViewById(R.id.layout_replace_recovery_phrase_transaction_info_safe_name, nameViewMock)
        viewHolderViewMock.mockFindViewById(R.id.layout_replace_recovery_phrase_transaction_info_safe_image, imageViewMock)
    }

    companion object {
        private val SAFE_ADDRESS = Solidity.Address(42.toBigInteger())

        private val SAFE_TRANSACTION = SafeTransaction(
            Transaction(
                address = Solidity.Address(100.toBigInteger())
            ), operation = TransactionExecutionRepository.Operation.DELEGATE_CALL
        )
    }
}
