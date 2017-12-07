package pm.gnosis.heimdall.ui.tokens.balances

import android.content.Context
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class TokenBalancesViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: TokenBalancesViewModel

    private val testAddress = BigInteger.ZERO

    @Before
    fun setUp() {
        viewModel = TokenBalancesViewModel(contextMock, tokenRepositoryMock)
    }

    @Test
    fun observeTokens() {
        val items = listOf<ERC20Token>()
        val testObserver = TestObserver<Result<Adapter.Data<ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val token = ERC20Token(testAddress, decimals = 18)
        val balance = BigInteger.ZERO
        val tokensWithBalances = arrayListOf(token to balance)
        val tokensWithBalancesMapped = arrayListOf(ERC20TokenWithBalance(token, balance))
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.just(tokensWithBalances))

        viewModel.setup(testAddress)
        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).should().loadTokenBalances(testAddress, items)
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
        val testObserver = TestObserver<Result<Adapter.Data<ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val account = Account(testAddress)
        val token = ERC20Token(testAddress, decimals = 18)
        val balance = BigInteger.ZERO
        val tokensWithBalances = arrayListOf(token to balance)
        val tokensWithBalancesMapped = arrayListOf(ERC20TokenWithBalance(token, balance))
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.just(tokensWithBalances))

        viewModel.setup(testAddress)
        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).should().loadTokenBalances(account.address, items)
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
        val testObserver = TestObserver<Result<Adapter.Data<ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val exception = Exception()
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.error(exception))

        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception).assertValueCount(1)
        testObserver.assertValueAt(0, {
            it is DataResult && it.data.parentId == null && it.data.diff == null &&
                    it.data.entries.isEmpty()
        })
        loadingObserver.assertNoValues()
    }

    @Test
    fun observeTokensLoadTokenBalancesError() {
        val items = listOf<ERC20Token>()
        val testObserver = TestObserver<Result<Adapter.Data<ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.just(Unit)
        val exception = Exception()
        val errorResult = ErrorResult<List<ERC20TokenWithBalance>>(exception)
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.error(exception))

        viewModel.setup(testAddress)
        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).should().loadTokenBalances(testAddress, items)
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
}
