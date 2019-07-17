package pm.gnosis.heimdall.ui.tokens.payment

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.net.UnknownHostException

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PaymentTokensViewModelTest {

    @JvmField
    @Rule
    val lifecycleRule = TestLifecycleRule()

    @Mock
    lateinit var tokenRepository: TokenRepository

    @Mock
    lateinit var context: Context

    private lateinit var viewModel: PaymentTokensViewModel

    @Before
    fun setup() {
        viewModel = PaymentTokensViewModel(context, testAppDispatchers, tokenRepository)
    }

    private fun List<ERC20Token>.tokensWithBalance() =
        mapIndexed { index, token ->
            token to if (token.address == TEST_TOKEN_2.address) null else index.toBigInteger() * BigInteger.TEN.pow(token.decimals)
        }

    @Test
    fun observeStateBalances() {
        val metric = PaymentTokensContract.MetricType.Balance(TEST_SAFE)
        val tokens = listOf(ERC20Token.ETHER_TOKEN, TEST_TOKEN)
        val loadTokensSingle = TestSingleFactory<List<ERC20Token>>()
        val expectedTokens = listOf(
            PaymentTokensContract.PaymentToken(ERC20Token.ETHER_TOKEN, "0", false),
            PaymentTokensContract.PaymentToken(TEST_TOKEN, "1", true)
        )
        given(tokenRepository.loadPaymentTokens()).willReturn(loadTokensSingle.get())
        given(tokenRepository.loadTokenBalances(MockUtils.any(), MockUtils.any())).willReturn(
            Observable.just(tokens.tokensWithBalance())
        )

        viewModel.setup(metric)
        // Only start loading once observed
        then(tokenRepository).shouldHaveZeroInteractions()

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)

        stateObserver
            .assertValues(PaymentTokensContract.State(emptyList(), true, null))
            .clear() // Clear after check
        then(tokenRepository).should().loadPaymentTokens()
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.success(tokens)

        stateObserver
            .assertValues(PaymentTokensContract.State(expectedTokens, false, null))
            .clear() // Clear after check
        then(tokenRepository).should().loadTokenBalances(TEST_SAFE, tokens)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Trigger the flow manually again
        val newTokens = tokens + TEST_TOKEN_2
        val newExpectedTokens = expectedTokens +
                PaymentTokensContract.PaymentToken(TEST_TOKEN_2, null, true)

        given(tokenRepository.loadTokenBalances(MockUtils.any(), MockUtils.any())).willReturn(
            Observable.just(newTokens.tokensWithBalance())
        )

        viewModel.loadPaymentTokens()
        stateObserver
            .assertValues(PaymentTokensContract.State(expectedTokens, true, null))
            .clear() // Clear after check
        then(tokenRepository).should(times(2)).loadPaymentTokens()
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.success(newTokens)

        stateObserver
            .assertValues(PaymentTokensContract.State(newExpectedTokens, false, null))
            .clear() // Clear after check
        then(tokenRepository).should().loadTokenBalances(TEST_SAFE, newTokens)
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeStateBalancesError() {
        context.mockGetString()
        val error = UnknownHostException()
        val metric = PaymentTokensContract.MetricType.Balance(TEST_SAFE)
        val loadTokensSingle = TestSingleFactory<List<ERC20Token>>()
        given(tokenRepository.loadPaymentTokens()).willReturn(loadTokensSingle.get())

        viewModel.setup(metric)
        // Only start loading once observed
        then(tokenRepository).shouldHaveZeroInteractions()

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)

        stateObserver
            .assertValues(PaymentTokensContract.State(emptyList(), true, null))
            .clear() // Clear after check
        then(tokenRepository).should().loadPaymentTokens()
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.error(error)

        stateObserver
            .assertValues(
                PaymentTokensContract.State(
                    emptyList(),
                    false,
                    PaymentTokensContract.ViewAction.ShowError(SimpleLocalizedException(R.string.error_check_internet_connection.toString()))
                )
            )
            .clear() // Clear after check
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeStateCreationFee() {
        val metric = PaymentTokensContract.MetricType.CreationFees(2)
        val tokensWithFee = listOf(
            ERC20Token.ETHER_TOKEN to Wei.ether("0.23").value,
            TEST_TOKEN to BigInteger.TEN * BigInteger.TEN.pow(TEST_TOKEN.decimals)
        )
        val loadTokensSingle = TestSingleFactory<List<Pair<ERC20Token, BigInteger>>>()
        val expectedTokens = listOf(
            PaymentTokensContract.PaymentToken(ERC20Token.ETHER_TOKEN, "0.23", true),
            PaymentTokensContract.PaymentToken(TEST_TOKEN, "10", true)
        )
        given(tokenRepository.loadPaymentTokensWithCreationFees(anyLong())).willReturn(loadTokensSingle.get())

        viewModel.setup(metric)
        // Only start loading once observed
        then(tokenRepository).shouldHaveZeroInteractions()

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)

        stateObserver
            .assertValues(PaymentTokensContract.State(emptyList(), true, null))
            .clear() // Clear after check
        then(tokenRepository).should().loadPaymentTokensWithCreationFees(2)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.success(tokensWithFee)

        stateObserver
            .assertValues(PaymentTokensContract.State(expectedTokens, false, null))
            .clear() // Clear after check
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Trigger the flow manually again
        val newTokensWithFee = tokensWithFee +
                (TEST_TOKEN_2 to  BigInteger.ONE * BigInteger.TEN.pow(TEST_TOKEN_2.decimals))
        val newExpectedTokens = expectedTokens +
                PaymentTokensContract.PaymentToken(TEST_TOKEN_2, "1", true)

        viewModel.loadPaymentTokens()
        stateObserver
            .assertValues(PaymentTokensContract.State(expectedTokens, true, null))
            .clear() // Clear after check
        then(tokenRepository).should(times(2)).loadPaymentTokensWithCreationFees(2)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.success(newTokensWithFee)

        stateObserver
            .assertValues(PaymentTokensContract.State(newExpectedTokens, false, null))
            .clear() // Clear after check
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeStateCreationFeeError() {
        context.mockGetString()
        val error = UnknownHostException()
        val metric = PaymentTokensContract.MetricType.CreationFees(1)
        val loadTokensSingle = TestSingleFactory<List<Pair<ERC20Token, BigInteger>>>()
        given(tokenRepository.loadPaymentTokensWithCreationFees(anyLong())).willReturn(loadTokensSingle.get())

        viewModel.setup(metric)
        // Only start loading once observed
        then(tokenRepository).shouldHaveZeroInteractions()

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)

        stateObserver
            .assertValues(PaymentTokensContract.State(emptyList(), true, null))
            .clear() // Clear after check
        then(tokenRepository).should().loadPaymentTokensWithCreationFees(1)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.error(error)

        stateObserver
            .assertValues(
                PaymentTokensContract.State(
                    emptyList(),
                    false,
                    PaymentTokensContract.ViewAction.ShowError(SimpleLocalizedException(R.string.error_check_internet_connection.toString()))
                )
            )
            .clear() // Clear after check
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeStateTransactionFee() {
        val metric = PaymentTokensContract.MetricType.TransactionFees(TEST_SAFE, TEST_TRANSACTION)
        val tokensWithFee = listOf(
            ERC20Token.ETHER_TOKEN to Wei.ether("0.23").value,
            TEST_TOKEN to BigInteger.TEN * BigInteger.TEN.pow(TEST_TOKEN.decimals)
        )
        val loadTokensSingle = TestSingleFactory<List<Pair<ERC20Token, BigInteger>>>()
        val expectedTokens = listOf(
            PaymentTokensContract.PaymentToken(ERC20Token.ETHER_TOKEN, "0.23", true),
            PaymentTokensContract.PaymentToken(TEST_TOKEN, "10", true)
        )
        given(tokenRepository.loadPaymentTokensWithTransactionFees(MockUtils.any(), MockUtils.any())).willReturn(loadTokensSingle.get())

        viewModel.setup(metric)
        // Only start loading once observed
        then(tokenRepository).shouldHaveZeroInteractions()

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)

        stateObserver
            .assertValues(PaymentTokensContract.State(emptyList(), true, null))
            .clear() // Clear after check
        then(tokenRepository).should().loadPaymentTokensWithTransactionFees(TEST_SAFE, TEST_TRANSACTION)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.success(tokensWithFee)

        stateObserver
            .assertValues(PaymentTokensContract.State(expectedTokens, false, null))
            .clear() // Clear after check
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Trigger the flow manually again
        val newTokensWithFee = tokensWithFee +
                (TEST_TOKEN_2 to  BigInteger.ONE * BigInteger.TEN.pow(TEST_TOKEN_2.decimals))
        val newExpectedTokens = expectedTokens +
                PaymentTokensContract.PaymentToken(TEST_TOKEN_2, "1", true)

        viewModel.loadPaymentTokens()
        stateObserver
            .assertValues(PaymentTokensContract.State(expectedTokens, true, null))
            .clear() // Clear after check
        then(tokenRepository).should(times(2)).loadPaymentTokensWithTransactionFees(TEST_SAFE, TEST_TRANSACTION)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.success(newTokensWithFee)

        stateObserver
            .assertValues(PaymentTokensContract.State(newExpectedTokens, false, null))
            .clear() // Clear after check
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeStateTransactionFeeError() {
        context.mockGetString()
        val error = UnknownHostException()
        val metric = PaymentTokensContract.MetricType.TransactionFees(TEST_SAFE, TEST_TRANSACTION)
        val loadTokensSingle = TestSingleFactory<List<Pair<ERC20Token, BigInteger>>>()
        given(tokenRepository.loadPaymentTokensWithTransactionFees(MockUtils.any(), MockUtils.any())).willReturn(loadTokensSingle.get())

        viewModel.setup(metric)
        // Only start loading once observed
        then(tokenRepository).shouldHaveZeroInteractions()

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)

        stateObserver
            .assertValues(PaymentTokensContract.State(emptyList(), true, null))
            .clear() // Clear after check
        then(tokenRepository).should().loadPaymentTokensWithTransactionFees(TEST_SAFE, TEST_TRANSACTION)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Finish loading
        loadTokensSingle.error(error)

        stateObserver
            .assertValues(
                PaymentTokensContract.State(
                    emptyList(),
                    false,
                    PaymentTokensContract.ViewAction.ShowError(SimpleLocalizedException(R.string.error_check_internet_connection.toString()))
                )
            )
            .clear() // Clear after check
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    private fun testLoadInitialPaymentToken(metric: PaymentTokensContract.MetricType, expectedSafe: Solidity.Address?) {
        given(tokenRepository.loadPaymentToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)
        stateObserver.clear()

        viewModel.setup(metric)

        val testObserver = TestLiveDataObserver<ERC20Token>()
        viewModel.paymentToken.observe(lifecycleRule, testObserver)

        // Check initial load
        then(tokenRepository).should().loadPaymentToken(expectedSafe)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        testObserver.assertValues(ERC20Token.ETHER_TOKEN)

        // Should emit last without loading from repo again
        val additionalObserver = TestLiveDataObserver<ERC20Token>()
        viewModel.paymentToken.observe(lifecycleRule, additionalObserver)

        additionalObserver.assertValues(ERC20Token.ETHER_TOKEN)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Should not have any state changes
        stateObserver.assertEmpty()
    }

    @Test
    fun loadInitialPaymentTokenBalance() {
        testLoadInitialPaymentToken(PaymentTokensContract.MetricType.Balance(TEST_SAFE), TEST_SAFE)
    }

    @Test
    fun loadInitialPaymentTransactionFee() {
        testLoadInitialPaymentToken(PaymentTokensContract.MetricType.TransactionFees(TEST_SAFE, TEST_TRANSACTION), TEST_SAFE)
    }

    @Test
    fun loadInitialPaymentCreationFee() {
        testLoadInitialPaymentToken(PaymentTokensContract.MetricType.CreationFees(2), null)
    }

    private fun testSetPaymentToken(metric: PaymentTokensContract.MetricType, expectedSafe: Solidity.Address?) {
        given(tokenRepository.loadPaymentToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(tokenRepository.setPaymentToken(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)
        stateObserver.clear()

        viewModel.setup(metric)

        val testObserver = TestLiveDataObserver<ERC20Token>()
        viewModel.paymentToken.observe(lifecycleRule, testObserver)

        // Check initial load
        testObserver.assertValues(ERC20Token.ETHER_TOKEN)
        then(tokenRepository).should().loadPaymentToken(expectedSafe)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Change payment token
        viewModel.setPaymentToken(TEST_TOKEN)

        testObserver.assertValues(ERC20Token.ETHER_TOKEN, TEST_TOKEN)
        then(tokenRepository).should().setPaymentToken(expectedSafe, TEST_TOKEN)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Should emit last without loading from repo again
        val additionalObserver = TestLiveDataObserver<ERC20Token>()
        viewModel.paymentToken.observe(lifecycleRule, additionalObserver)

        additionalObserver.assertValues(TEST_TOKEN)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Should not have any state changes
        stateObserver.assertEmpty()
    }

    @Test
    fun setPaymentTokenBalance() {
        testSetPaymentToken(PaymentTokensContract.MetricType.Balance(TEST_SAFE), TEST_SAFE)
    }

    @Test
    fun setPaymentTokenTransactionFee() {
        testSetPaymentToken(PaymentTokensContract.MetricType.TransactionFees(TEST_SAFE, TEST_TRANSACTION), TEST_SAFE)
    }

    @Test
    fun setPaymentTokenCreationFee() {
        testSetPaymentToken(PaymentTokensContract.MetricType.CreationFees(2), null)
    }

    @Test
    fun setPaymentTokenError() {
        context.mockGetString()
        given(tokenRepository.loadPaymentToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        val error = UnknownHostException()
        given(tokenRepository.setPaymentToken(MockUtils.any(), MockUtils.any())).willReturn(Completable.error(error))

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)
        stateObserver.clear()

        viewModel.setup(PaymentTokensContract.MetricType.Balance(TEST_SAFE))

        val testObserver = TestLiveDataObserver<ERC20Token>()
        viewModel.paymentToken.observe(lifecycleRule, testObserver)

        // Check initial load
        testObserver.assertValues(ERC20Token.ETHER_TOKEN)
        then(tokenRepository).should().loadPaymentToken(TEST_SAFE)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Change payment token
        viewModel.setPaymentToken(TEST_TOKEN)

        testObserver.assertValues(ERC20Token.ETHER_TOKEN)
        then(tokenRepository).should().setPaymentToken(TEST_SAFE, TEST_TOKEN)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        // Should display error via state changes
        stateObserver
            .assertValueCount(1)
            .assertValueAt(0) {
                assertTrue(it.viewAction is PaymentTokensContract.ViewAction.ShowError)
                val viewError = (it.viewAction as PaymentTokensContract.ViewAction.ShowError).error
                assertTrue(viewError is LocalizedException)
                assertEquals((viewError as LocalizedException).localizedMessage(), R.string.error_check_internet_connection.toString())
            }
    }

    @Test
    fun loadInitialPaymentError() {
        context.mockGetString()
        val error = UnknownHostException()
        given(tokenRepository.loadPaymentToken(MockUtils.any())).willReturn(Single.error(error))
        given(tokenRepository.setPaymentToken(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        val stateObserver = TestLiveDataObserver<PaymentTokensContract.State>()
        viewModel.state.observe(lifecycleRule, stateObserver)
        stateObserver.clear()

        viewModel.setup(PaymentTokensContract.MetricType.Balance(TEST_SAFE))

        val testObserver = TestLiveDataObserver<ERC20Token>()
        viewModel.paymentToken.observe(lifecycleRule, testObserver)

        // Check initial load
        then(tokenRepository).should().loadPaymentToken(TEST_SAFE)
        then(tokenRepository).shouldHaveNoMoreInteractions()

        testObserver.assertEmpty()

        // Should display error via state changes
        stateObserver
            .assertValueCount(1)
            .assertValueAt(0) {
                assertTrue(it.viewAction is PaymentTokensContract.ViewAction.ShowError)
                val viewError = (it.viewAction as PaymentTokensContract.ViewAction.ShowError).error
                assertTrue(viewError is LocalizedException)
                assertEquals((viewError as LocalizedException).localizedMessage(), R.string.error_check_internet_connection.toString())
            }

        // Change payment token should still be propagated
        viewModel.setPaymentToken(TEST_TOKEN)

        testObserver.assertValues(TEST_TOKEN)
        then(tokenRepository).should().setPaymentToken(TEST_SAFE, TEST_TOKEN)
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private val TEST_TOKEN = ERC20Token(Solidity.Address(BigInteger.ONE), "Hello Token", "HT", 10)
        private val TEST_TOKEN_2 = ERC20Token(Solidity.Address(BigInteger.TEN), "Another Token", "AT", 12)
        private val TEST_TRANSACTION = testSafeTransaction(TEST_SAFE)
    }
}
