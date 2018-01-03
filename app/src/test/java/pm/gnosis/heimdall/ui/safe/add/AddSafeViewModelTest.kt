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
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.MockUtils.any
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.models.Currency
import java.math.BigDecimal
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddSafeViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var tickerRepositoryMock: TickerRepository

    private lateinit var viewModel: AddSafeViewModel

    @Before
    fun setUp() {
        context.mockGetString()
        viewModel = AddSafeViewModel(context, safeRepositoryMock, tickerRepositoryMock)
    }

    @Test
    fun addExistingSafe() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(safeRepositoryMock.add(any(), anyString())).willReturn(Completable.complete())

        viewModel.addExistingSafe("test", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(DataResult(Unit))
        then(safeRepositoryMock).should().add(BigInteger.ZERO, "test")
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingSafeEmptyName() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.addExistingSafe("", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.message == R.string.error_blank_name.toString()
        }
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingInvalidAddress() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.addExistingSafe("test", "test").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.message == R.string.invalid_ethereum_address.toString()
        }
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingRepoError() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val error = IllegalStateException()
        given(safeRepositoryMock.add(any(), anyString())).willReturn(Completable.error(error))

        viewModel.addExistingSafe("test", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error == error
        }
        then(safeRepositoryMock).should().add(BigInteger.ZERO, "test")
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun deployNewSafe() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(safeRepositoryMock.deploy(any(), any(), anyInt())).willReturn(Completable.complete())

        viewModel.deployNewSafe("test").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(DataResult(Unit))
        then(safeRepositoryMock).should().deploy("test", emptySet(), 1)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun deployNewSafeEmptyName() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.deployNewSafe("").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.message == R.string.error_blank_name.toString()
        }
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun deployNewSafeRepoError() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val error = IllegalStateException()
        given(safeRepositoryMock.deploy(any(), any(), anyInt())).willReturn(Completable.error(error))

        viewModel.deployNewSafe("test").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error == error
        }
        then(safeRepositoryMock).should().deploy("test", emptySet(), 1)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeEstimate() {
        val testObserver = TestObserver.create<Wei>()
        val costs = Wei(BigInteger.TEN)
        given(safeRepositoryMock.estimateDeployCosts(any(), anyInt())).willReturn(Single.just(costs))
        viewModel.observeEstimate().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(costs).assertTerminated()
        then(safeRepositoryMock).should().estimateDeployCosts(emptySet(), 1)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeEstimateError() {
        val testObserver = TestObserver.create<Wei>()
        val error = IllegalStateException()
        given(safeRepositoryMock.estimateDeployCosts(any(), anyInt())).willReturn(Single.error(error))
        viewModel.observeEstimate().subscribe(testObserver)

        testObserver.assertError(error).assertNoValues().assertTerminated()
        then(safeRepositoryMock).should().estimateDeployCosts(emptySet(), 1)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFiatConversion() {
        val wei = Wei(BigInteger.ONE)
        val testObserver = TestObserver.create<Result<Pair<BigDecimal, Currency>>>()
        val result = BigDecimal.ONE to Currency("testId", "testName", "testSymbol", 0, 0, BigDecimal.ZERO, Currency.FiatTicker.USD)
        given(tickerRepositoryMock.convertToFiat(MockUtils.any<Wei>(), MockUtils.any())).willReturn(Single.just(result))

        viewModel.loadFiatConversion(wei).subscribe(testObserver)

        then(tickerRepositoryMock).should().convertToFiat(wei)
        then(tickerRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(DataResult(result))
    }

    @Test
    fun loadFiatConversionError() {
        val wei = Wei(BigInteger.ONE)
        val exception = Exception()
        val testObserver = TestObserver.create<Result<Pair<BigDecimal, Currency>>>()
        given(tickerRepositoryMock.convertToFiat(MockUtils.any<Wei>(), MockUtils.any())).willReturn(Single.error(exception))

        viewModel.loadFiatConversion(wei).subscribe(testObserver)

        then(tickerRepositoryMock).should().convertToFiat(wei)
        then(tickerRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }
}
