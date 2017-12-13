package pm.gnosis.heimdall.ui.safe.add

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils.any
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddSafeViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var repository: GnosisSafeRepository

    private lateinit var viewModel: AddSafeViewModel

    @Before
    fun setUp() {
        context.mockGetString()
        viewModel = AddSafeViewModel(context, repository)
    }

    @Test
    fun addExistingSafe() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(repository.add(any(), anyString())).willReturn(Completable.complete())

        viewModel.addExistingSafe("test", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(DataResult(Unit))
        then(repository).should().add(BigInteger.ZERO, "test")
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingSafeEmptyName() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.addExistingSafe("", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.message == R.string.error_blank_name.toString()
        }
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingInvalidAddress() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.addExistingSafe("test", "test").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.message == R.string.invalid_ethereum_address.toString()
        }
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingRepoError() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val error = IllegalStateException()
        given(repository.add(any(), anyString())).willReturn(Completable.error(error))

        viewModel.addExistingSafe("test", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error == error
        }
        then(repository).should().add(BigInteger.ZERO, "test")
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun deployNewSafe() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(repository.deploy(any(), any(), anyInt())).willReturn(Completable.complete())

        viewModel.deployNewSafe("test").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(DataResult(Unit))
        then(repository).should().deploy("test", emptySet(), 1)
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun deployNewSafeEmptyName() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.deployNewSafe("").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.message == R.string.error_blank_name.toString()
        }
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun deployNewSafeRepoError() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val error = IllegalStateException()
        given(repository.deploy(any(), any(), anyInt())).willReturn(Completable.error(error))

        viewModel.deployNewSafe("test").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error == error
        }
        then(repository).should().deploy("test", emptySet(), 1)
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeEstimate() {
        val testObserver = TestObserver.create<Wei>()
        val costs = Wei(BigInteger.TEN)
        given(repository.estimateDeployCosts(any(), anyInt())).willReturn(Single.just(costs))
        viewModel.observeEstimate().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(costs).assertTerminated()
        then(repository).should().estimateDeployCosts(emptySet(), 1)
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeEstimateError() {
        val testObserver = TestObserver.create<Wei>()
        val error = IllegalStateException()
        given(repository.estimateDeployCosts(any(), anyInt())).willReturn(Single.error(error))
        viewModel.observeEstimate().subscribe(testObserver)

        testObserver.assertError(error).assertNoValues().assertTerminated()
        then(repository).should().estimateDeployCosts(emptySet(), 1)
        then(repository).shouldHaveNoMoreInteractions()
    }

}