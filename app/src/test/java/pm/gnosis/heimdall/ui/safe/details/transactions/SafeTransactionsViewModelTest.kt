package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Context
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeTransactionsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    private lateinit var viewModel: SafeTransactionsViewModel

    private var testAddress = BigInteger.ZERO
    private var invalidAddress = "1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".hexAsBigInteger()
    private var testName = "testName"

    @Before
    fun setup() {
        viewModel = SafeTransactionsViewModel(safeRepository)
    }

    @Test
    fun testSetupSameAddress() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Single.just(0))

        viewModel.setup(testAddress)
        viewModel.initTransaction(false).subscribe(TestObserver())

        // Setting the same address should keep the cache
        viewModel.setup(testAddress)
        viewModel.initTransaction(false).subscribe(TestObserver())

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testSetupDifferentAddress() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Single.just(0))

        viewModel.setup(testAddress)
        viewModel.initTransaction(false).subscribe(TestObserver())

        // Setting a different address should clear the cache
        viewModel.setup(BigInteger.TEN)
        viewModel.initTransaction(false).subscribe(TestObserver())

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).should(times(1)).loadDescriptionCount(BigInteger.TEN)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initTransaction() {
    }

    @Test
    fun observeTransaction() {
    }

}