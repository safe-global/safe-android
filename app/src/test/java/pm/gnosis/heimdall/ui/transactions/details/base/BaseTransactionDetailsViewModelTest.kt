package pm.gnosis.heimdall.ui.transactions.details.base

import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
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
    fun testObserveSafe() {
        val safeProcessor = PublishProcessor.create<Safe>()
        given(safeRepositoryMock.observeSafe(MockUtils.any())).willReturn(safeProcessor)
        val testObserver = TestObserver<Safe>()

        viewModel.observeSafe(TEST_SAFE).subscribe(testObserver)
        testObserver.assertEmpty()

        safeProcessor.offer(Safe(TEST_SAFE, "Some Safe"))
        testObserver.assertValuesOnly(Safe(TEST_SAFE, "Some Safe"))

        safeProcessor.offer(Safe(TEST_SAFE, "Some Updated Safe"))
        testObserver.assertValuesOnly(
            Safe(TEST_SAFE, "Some Safe"),
            Safe(TEST_SAFE, "Some Updated Safe") // New value
        )

        val error = IllegalStateException()
        safeProcessor.onError(error)
        testObserver.assertFailure(
            Predicate { it == error },
            Safe(TEST_SAFE, "Some Safe"),
            Safe(TEST_SAFE, "Some Updated Safe")
        )

    }

    companion object {
        private val TEST_SAFE = BigInteger.TEN
    }
}
