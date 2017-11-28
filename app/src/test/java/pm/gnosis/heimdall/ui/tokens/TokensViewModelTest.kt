package pm.gnosis.heimdall.ui.tokens

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.tokens.overview.TokensContract
import pm.gnosis.heimdall.ui.tokens.overview.TokensViewModel
import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class TokensViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var accountsRepository: AccountsRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: TokensViewModel

    private val testAddress = BigInteger.ZERO

    @Before
    fun setUp() {
        viewModel = TokensViewModel(contextMock, accountsRepository, tokenRepositoryMock)
        given(contextMock.getString(anyInt())).willReturn("")
    }

    @Test
    fun observeTokens() {
        val items = listOf<ERC20Token>()
        val testObserver = TestObserver<Result<Adapter.Data<TokensContract.ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val account = Account(testAddress)
        val token = ERC20Token(testAddress, decimals = 18)
        val balance = BigInteger.ZERO
        val tokensWithBalances = arrayListOf(token to balance)
        val tokensWithBalancesMapped = arrayListOf(TokensContract.ERC20TokenWithBalance(token, balance))
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(account))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.just(tokensWithBalances))

        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(accountsRepository).should().loadActiveAccount()
        then(tokenRepositoryMock).should().loadTokenBalances(account.address, items)
        then(accountsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValueCount(2)
        val values = testObserver.values()
        testObserver.assertValueAt(0, {
            it is DataResult && it.data.parentId == null && it.data.diff == null &&
                    it.data.entries.isEmpty()
        })

        testObserver.assertValueAt(1, {
            it is DataResult && it.data.parentId == (values[0] as DataResult).data.id &&
                    it.data.diff != null && it.data.entries == tokensWithBalancesMapped
        })
        loadingObserver.assertValues(true, false)
    }

    @Test
    fun observeTokensWithAddress() {
        val items = listOf<ERC20Token>()
        val testObserver = TestObserver<Result<Adapter.Data<TokensContract.ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val account = Account(testAddress)
        val token = ERC20Token(testAddress, decimals = 18)
        val balance = BigInteger.ZERO
        val tokensWithBalances = arrayListOf(token to balance)
        val tokensWithBalancesMapped = arrayListOf(TokensContract.ERC20TokenWithBalance(token, balance))
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.just(tokensWithBalances))

        viewModel.setup(testAddress)
        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).should().loadTokenBalances(account.address, items)
        then(accountsRepository).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValueCount(2)
        val values = testObserver.values()
        testObserver.assertValueAt(0, {
            it is DataResult && it.data.parentId == null && it.data.diff == null &&
                    it.data.entries.isEmpty()
        })

        testObserver.assertValueAt(1, {
            it is DataResult && it.data.parentId == (values[0] as DataResult).data.id &&
                    it.data.diff != null && it.data.entries == tokensWithBalancesMapped
        })
        loadingObserver.assertValues(true, false)
    }

    @Test
    fun observeTokensErrorObserveTokens() {
        val testObserver = TestObserver<Result<Adapter.Data<TokensContract.ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val exception = Exception()
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.error(exception))

        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(accountsRepository).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception).assertValueCount(1)
        testObserver.assertValueAt(0, {
            it is DataResult && it.data.parentId == null && it.data.diff == null &&
                    it.data.entries.isEmpty()
        })
        loadingObserver.assertNoValues()
    }

    @Test
    fun observeTokensLoadActiveAccountError() {
        val items = listOf<ERC20Token>()
        val testObserver = TestObserver<Result<Adapter.Data<TokensContract.ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val exception = Exception()
        val errorResult = ErrorResult<List<TokensContract.ERC20TokenWithBalance>>(exception)
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error(exception))

        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(accountsRepository).should().loadActiveAccount()
        then(accountsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValueCount(2)
        testObserver.assertValueAt(0, {
            it is DataResult && it.data.parentId == null && it.data.diff == null &&
                    it.data.entries.isEmpty()
        })

        testObserver.assertValueAt(1, {
            it == errorResult
        })
        loadingObserver.assertNoValues()
    }

    @Test
    fun observeTokensLoadTokenBalancesError() {
        val items = listOf<ERC20Token>()
        val testObserver = TestObserver<Result<Adapter.Data<TokensContract.ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val account = Account(testAddress)
        val exception = Exception()
        val errorResult = ErrorResult<List<TokensContract.ERC20TokenWithBalance>>(exception)
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(account))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.error(exception))

        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(accountsRepository).should().loadActiveAccount()
        then(tokenRepositoryMock).should().loadTokenBalances(account.address, items)
        then(accountsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValueCount(2)
        testObserver.assertValueAt(0, {
            it is DataResult && it.data.parentId == null && it.data.diff == null &&
                    it.data.entries.isEmpty()
        })

        testObserver.assertValueAt(1, {
            it == errorResult
        })
        loadingObserver.assertValues(true, false)
    }

    @Test
    fun loadTokenInfo() {
        val token = ERC20Token(testAddress, decimals = 0)
        val result = DataResult(token)
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.just(token))

        viewModel.loadTokenInfo(token).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(result)
    }

    @Test
    fun loadTokenInfoError() {
        val token = ERC20Token(testAddress, decimals = 0)
        val exception = InvalidAddressException()
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadTokenInfo(token).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue { it is ErrorResult && it.error is LocalizedException }
    }

    @Test
    fun removeToken() {
        val token = ERC20Token(testAddress, decimals = 0)
        val result = DataResult(token)
        val testCompletable = TestCompletable()
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.removeToken(MockUtils.any())).willReturn(testCompletable)

        viewModel.removeToken(token).subscribe(testObserver)

        assertEquals(1, testCompletable.callCount)
        then(tokenRepositoryMock).should().removeToken(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(result)
    }

    @Test
    fun removeTokenError() {
        val token = ERC20Token(testAddress, decimals = 0)
        val exception = Exception()
        val result = ErrorResult<ERC20Token>(exception)
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.removeToken(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.removeToken(token).subscribe(testObserver)

        then(tokenRepositoryMock).should().removeToken(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(result)
    }
}
