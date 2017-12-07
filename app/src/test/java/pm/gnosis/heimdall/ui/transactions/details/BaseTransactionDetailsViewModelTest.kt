package pm.gnosis.heimdall.ui.transactions.details

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
import pm.gnosis.heimdall.ui.transactions.details.BaseTransactionDetailsContract.State
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class BaseTransactionDetailsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var safeRepositoryMock: GnosisSafeRepository

    private lateinit var viewModel: BaseTransactionDetailsViewModel

    @Before
    fun setUp() {
        viewModel = BaseTransactionDetailsViewModel(safeRepositoryMock)
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
                .assertValueCount(1).assertValueAt(0, State(TEST_DEFAULT_SAFE, firstSafeList))

        // Update selected safe
        viewModel.updateSelectedSafe(BigInteger.ONE)

        // Return list and newly selected safe (even if not in list)
        testProcessor.onNext(firstSafeList)
        testSubscriber.assertNoErrors()
                .assertValueCount(2).assertValueAt(1, State(BigInteger.ONE, firstSafeList))

        // Check that a new subscriber get the correct selected safe
        val testSubscriber2 = TestSubscriber<State>()
        viewModel.observeSafes(TEST_DEFAULT_SAFE).subscribe(testSubscriber2)
        testSubscriber2.assertNoErrors()
                .assertValueCount(1).assertValueAt(0, State(BigInteger.ONE, firstSafeList))


        // Reset selected safe
        viewModel.updateSelectedSafe(null)
        val secondSafeList = listOf(Safe(BigInteger.ZERO), Safe(BigInteger.TEN), Safe(BigInteger.ONE))
        // Return new list and default safe
        testProcessor.onNext(secondSafeList)

        // Assert that both subscribers got the correct data
        testSubscriber.assertNoErrors()
                .assertValueCount(3).assertValueAt(2, State(TEST_DEFAULT_SAFE, secondSafeList))
        testSubscriber2.assertNoErrors()
                .assertValueCount(2).assertValueAt(1, State(TEST_DEFAULT_SAFE, secondSafeList))

    }

    companion object {
        private val TEST_DEFAULT_SAFE = BigInteger.TEN
    }
}