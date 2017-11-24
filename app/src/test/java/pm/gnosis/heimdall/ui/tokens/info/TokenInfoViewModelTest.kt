package pm.gnosis.heimdall.ui.tokens.info

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class TokenInfoViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: TokenInfoViewModel

    private val testToken = ERC20Token(BigInteger.ZERO, decimals = 18)

    @Before
    fun setup() {
        viewModel = TokenInfoViewModel(tokenRepositoryMock)
    }

    @Test
    fun testObserveToken() {
        val testObserver = TestObserver<ERC20Token>()
        given(tokenRepositoryMock.observeToken(MockUtils.any())).willReturn(Flowable.just(testToken))

        viewModel.observeToken(BigInteger.ZERO).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeToken(BigInteger.ZERO)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(testToken).assertNoErrors()
    }

    @Test
    fun testObserveTokenError() {
        val testObserver = TestObserver<ERC20Token>()
        val exception = Exception()
        given(tokenRepositoryMock.observeToken(MockUtils.any())).willReturn(Flowable.error(exception))

        viewModel.observeToken(BigInteger.ZERO).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeToken(BigInteger.ZERO)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun testRemoveToken() {
        val testCompletable = TestCompletable()
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.observeToken(MockUtils.any())).willReturn(Flowable.just(testToken))
        given(tokenRepositoryMock.removeToken(MockUtils.any())).willReturn(testCompletable)

        viewModel.observeToken(BigInteger.ZERO).subscribe(TestObserver())
        viewModel.removeToken().subscribe(testObserver)

        then(tokenRepositoryMock).should().observeToken(BigInteger.ZERO)
        then(tokenRepositoryMock).should().removeToken(BigInteger.ZERO)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, testCompletable.callCount)
        testObserver
                .assertValue(DataResult(testToken))
                .assertNoErrors()
    }

    @Test
    fun testRemoveTokenError() {
        val testObserver = TestObserver<Result<ERC20Token>>()
        val exception = Exception()
        given(tokenRepositoryMock.observeToken(MockUtils.any())).willReturn(Flowable.just(testToken))
        given(tokenRepositoryMock.removeToken(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.observeToken(BigInteger.ZERO).subscribe(TestObserver())
        viewModel.removeToken().subscribe(testObserver)

        then(tokenRepositoryMock).should().observeToken(BigInteger.ZERO)
        then(tokenRepositoryMock).should().removeToken(BigInteger.ZERO)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver
                .assertValue(ErrorResult(exception))
                .assertNoErrors()
    }
}
