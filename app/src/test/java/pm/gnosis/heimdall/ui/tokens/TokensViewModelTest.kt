package pm.gnosis.heimdall.ui.tokens

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.util.DataResult
import pm.gnosis.heimdall.common.util.ErrorResult
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.test.utils.TestCompletable
import pm.gnosis.utils.hexAsBigInteger

@RunWith(MockitoJUnitRunner::class)
class TokensViewModelTest {
    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: TokensViewModel

    private val testAddress = "0x0000000000000000000000000000000000000000".hexAsBigInteger()

    @Before
    fun setUp() {
        viewModel = TokensViewModel(contextMock, tokenRepositoryMock)
    }

    @Test
    fun observeTokens() {
        val items = listOf<ERC20Token>()
        val testSubscriber = TestSubscriber<List<ERC20Token>>()
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))

        viewModel.observeTokens().subscribe(testSubscriber)

        then(tokenRepositoryMock).should().observeTokens()
        testSubscriber.assertNoErrors().assertValue(items)
    }

    @Test
    fun observeTokensError() {
        val exception = Exception()
        val testSubscriber = TestSubscriber<List<ERC20Token>>()
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.error(exception))

        viewModel.observeTokens().subscribe(testSubscriber)

        then(tokenRepositoryMock).should().observeTokens()
        testSubscriber.assertNoValues().assertError(exception)
    }

    @Test
    fun loadTokenInfo() {
        val token = ERC20Token(testAddress)
        val result = DataResult(token)
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.just(token))

        viewModel.loadTokenInfo(token).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        testObserver.assertNoErrors().assertValue(result)
    }

    @Test
    fun loadTokenInfoError() {
        val token = ERC20Token(testAddress)
        val exception = Exception()
        val result = ErrorResult<ERC20Token>(exception)
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadTokenInfo(token).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        testObserver.assertNoErrors().assertValue(result)
    }

    @Test
    fun addToken() {
        val token = ERC20Token(testAddress)
        val result = DataResult(token)
        val testObserver = TestObserver<Result<ERC20Token>>()
        val testCompletable = TestCompletable()
        given(tokenRepositoryMock.addToken(MockUtils.any(), any())).willReturn(testCompletable)

        viewModel.addToken(testAddress).subscribe(testObserver)

        assertEquals(1, testCompletable.callCount)
        then(tokenRepositoryMock).should().addToken(testAddress)
        testObserver.assertNoErrors().assertValue(result)
    }

    @Test
    fun addTokenError() {
        val exception = Exception()
        val result = ErrorResult<ERC20Token>(exception)
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.addToken(MockUtils.any(), any())).willReturn(Completable.error(exception))

        viewModel.addToken(testAddress).subscribe(testObserver)

        then(tokenRepositoryMock).should().addToken(testAddress)
        testObserver.assertNoErrors().assertValue(result)
    }

    @Test
    fun removeToken() {
        val token = ERC20Token(testAddress)
        val result = DataResult(token)
        val testCompletable = TestCompletable()
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.removeToken(MockUtils.any())).willReturn(testCompletable)

        viewModel.removeToken(token).subscribe(testObserver)

        assertEquals(1, testCompletable.callCount)
        then(tokenRepositoryMock).should().removeToken(testAddress)
        testObserver.assertNoErrors().assertValue(result)
    }

    @Test
    fun removeTokenError() {
        val token = ERC20Token(testAddress)
        val exception = Exception()
        val result = ErrorResult<ERC20Token>(exception)
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.removeToken(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.removeToken(token).subscribe(testObserver)

        then(tokenRepositoryMock).should().removeToken(testAddress)
        testObserver.assertNoErrors().assertValue(result)
    }
}
