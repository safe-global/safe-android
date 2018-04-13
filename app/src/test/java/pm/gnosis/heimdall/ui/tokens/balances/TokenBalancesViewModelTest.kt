package pm.gnosis.heimdall.ui.tokens.balances

import android.content.Context
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
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

    private val testAddress = Solidity.Address(BigInteger.ZERO)

    @Before
    fun setUp() {
        viewModel = TokenBalancesViewModel(contextMock, tokenRepositoryMock)
    }

    @Test
    fun observeTokens() {
        val items = listOf<ERC20Token>()
        val testObserver = TestObserver<Result<Adapter.Data<ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.empty<Unit>()
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
        then(tokenRepositoryMock).should().loadTokenBalances(testAddress, listOf(ERC20Token.ETHER_TOKEN) + items)
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
        val refreshEvents = Observable.empty<Unit>()
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
        then(tokenRepositoryMock).should().loadTokenBalances(testAddress, listOf(ERC20Token.ETHER_TOKEN) + items)
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
        val refreshEvents = Observable.empty<Unit>()
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
    fun observeTokensLoadTokenBalancesErrorOnInitialLoad() {
        val items = listOf<ERC20Token>()
        val etherTokenList = listOf(ERC20Token.ETHER_TOKEN) + items
        val testObserver = TestObserver<Result<Adapter.Data<ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEvents = Observable.empty<Unit>()
        val exception = Exception()
        val errorResult = ErrorResult<List<ERC20TokenWithBalance>>(exception)
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.error(exception))

        viewModel.setup(testAddress)
        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEvents).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).should().loadTokenBalances(testAddress, etherTokenList)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        val values = testObserver.values()
        testObserver.assertValueAt(0, {
            it is DataResult && it.data.parentId == null && it.data.diff == null &&
                    it.data.entries.isEmpty()
        })

        testObserver.assertValueAt(1, {
            it is DataResult && it.data.parentId == (values[0] as DataResult).data.id &&
                    it.data.diff != null && it.data.entries == etherTokenList.map { ERC20TokenWithBalance(it, null) }
        })

        testObserver.assertValueAt(2, {
            it == errorResult
        })
        testObserver.assertNoErrors().assertValueCount(3)
        loadingObserver.assertValues(true, false)
    }

    @Test
    fun observeTokensLoadTokenBalancesErrorOnSubsequentLoad() {
        val items = listOf<ERC20Token>()
        val etherTokenList = listOf(ERC20Token.ETHER_TOKEN) + items
        val testObserver = TestObserver<Result<Adapter.Data<ERC20TokenWithBalance>>>()
        val loadingObserver = TestObserver<Boolean>()
        val refreshEventsSubject = PublishSubject.create<Unit>()

        val token = ERC20Token(testAddress, decimals = 18)
        val balance = BigInteger.ZERO
        val tokensWithBalances = arrayListOf(token to balance)
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.just(items))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.just(tokensWithBalances))

        viewModel.setup(testAddress)
        viewModel.observeLoadingStatus().subscribe(loadingObserver)
        viewModel.observeTokens(refreshEventsSubject).subscribe(testObserver)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).should().loadTokenBalances(testAddress, etherTokenList)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValueCount(2)
        val values = testObserver.values()
        testObserver.assertValueAt(0, {
            it is DataResult && it.data.parentId == null && it.data.diff == null &&
                    it.data.entries.isEmpty()
        })

        testObserver.assertValueAt(1, {
            it is DataResult && it.data.parentId == (values[0] as DataResult).data.id &&
                    it.data.diff != null && it.data.entries == tokensWithBalances.map { ERC20TokenWithBalance(it.first, it.second) }
        })
        loadingObserver.assertValues(true, false)

        // Reload data without error
        refreshEventsSubject.onNext(Unit)
        testObserver.assertNoErrors().assertValueCount(3)

        testObserver.assertValueAt(2, {
            it is DataResult && it.data.parentId == (values[1] as DataResult).data.id &&
                    it.data.diff != null && it.data.entries == tokensWithBalances.map { ERC20TokenWithBalance(it.first, it.second) }
        })
        loadingObserver.assertValues(true, false, true, false)

        // Reload data with error
        val exception = Exception()
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), anyList())).willReturn(Observable.error(exception))
        refreshEventsSubject.onNext(Unit)
        testObserver.assertNoErrors().assertValueCount(4)
        testObserver.assertValueAt(3, {
            it == ErrorResult<List<ERC20TokenWithBalance>>(exception)
        })
        loadingObserver.assertValues(true, false, true, false, true, false)
    }
}
