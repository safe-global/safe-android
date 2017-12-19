package pm.gnosis.heimdall.ui.transactions.details.base

import io.reactivex.processors.BehaviorProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.transactions.details.base.BaseEditableTransactionDetailsContract.State
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class BaseEditableTransactionDetailsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var safeRepositoryMock: GnosisSafeRepository

    private lateinit var viewModel: BaseEditableTransactionDetailsViewModel

    @Before
    fun setUp() {
        viewModel = BaseEditableTransactionDetailsViewModel(safeRepositoryMock)
    }

    @Test
    fun observeSafesAndSetSelected() {
        val testProcessor = BehaviorProcessor.create<List<Safe>>()
        BDDMockito.given(safeRepositoryMock.observeDeployedSafes()).willReturn(testProcessor)

        val testSubscriber = TestSubscriber<State>()
        viewModel.observeSafes(TEST_DEFAULT_SAFE).subscribe(testSubscriber)
        testSubscriber.assertEmpty()

        // Return list and default safe (even if not in list)
        val firstSafeList = listOf(Safe(BigInteger.ZERO), Safe(BigInteger.ONE))
        testProcessor.onNext(firstSafeList)
        testSubscriber.assertNoErrors()
                .assertValueCount(1).assertValueAt(0, State(0, firstSafeList))

        // Update selected safe
        viewModel.updateSelectedSafe(BigInteger.ONE)

        // Return list and newly selected safe (even if not in list)
        testProcessor.onNext(firstSafeList)
        testSubscriber.assertNoErrors()
                .assertValueCount(2).assertValueAt(1, State(1, firstSafeList))

        // Check that a new subscriber gets the correct selected safe
        val testSubscriber2 = TestSubscriber<State>()
        viewModel.observeSafes(TEST_DEFAULT_SAFE).subscribe(testSubscriber2)
        testSubscriber2.assertNoErrors()
                .assertValueCount(1).assertValueAt(0, State(1, firstSafeList))


        // Reset selected safe
        viewModel.updateSelectedSafe(null)
        val secondSafeList = listOf(Safe(BigInteger.ZERO), Safe(BigInteger.ONE), Safe(BigInteger.TEN))
        // Return new list and default safe
        testProcessor.onNext(secondSafeList)

        // Assert that both subscribers got the correct data
        testSubscriber.assertNoErrors()
                .assertValueCount(3).assertValueAt(2, State(2, secondSafeList))
        testSubscriber2.assertNoErrors()
                .assertValueCount(2).assertValueAt(1, State(2, secondSafeList))

        // Check that a new subscriber without a default safe gets the correct selected safe
        val testSubscriber3 = TestSubscriber<State>()
        viewModel.observeSafes(null).subscribe(testSubscriber3)
        testSubscriber3.assertNoErrors()
                .assertValueCount(1).assertValueAt(0, State(0, secondSafeList))

    }

    companion object {
        private val TEST_DEFAULT_SAFE = BigInteger.TEN
    }
}