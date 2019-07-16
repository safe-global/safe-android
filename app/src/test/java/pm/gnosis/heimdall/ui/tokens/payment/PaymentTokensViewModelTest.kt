package pm.gnosis.heimdall.ui.tokens.payment

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.reactivex.Completable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.utils.asEthereumAddress

@RunWith(MockitoJUnitRunner::class)
class PaymentTokensViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var paymentTokensViewModel: PaymentTokensViewModel

    @Mock
    lateinit var tokenRepository: TokenRepository

    @Mock
    lateinit var context: Context

    var testDispatcher = TestCoroutineDispatcher()

    private val dispatchers = ApplicationModule.AppCoroutineDispatchers(
        testDispatcher,
        testDispatcher,
        testDispatcher,
        testDispatcher,
        testDispatcher
    )

    @Before
    fun setup() {
        paymentTokensViewModel = PaymentTokensViewModel(context, dispatchers, tokenRepository)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        // reset main after the test is done
        Dispatchers.resetMain()
        // call this to ensure TestCoroutineDispater doesn't
        // accidentally carry state to the next test
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun test_SendPaymentToken() {

        val token = ERC20Token.ETHER_TOKEN

        paymentTokensViewModel.setup(TEST_SAFE, PaymentTokensContract.MetricType.Balance)

        given(tokenRepository.setPaymentToken(TEST_SAFE, token)).willReturn(Completable.complete())

        paymentTokensViewModel.setPaymentToken(token)


        paymentTokensViewModel.paymentToken.observeForTesting {
            assertEquals(token, paymentTokensViewModel.paymentToken.value)
        }
    }

    // helper method to allow us to get the value from a LiveData
    // LiveData won't publish a result until there is at least one observer
    private fun <T> LiveData<T>.observeForTesting(
        block: () -> Unit
    ) {
        val observer = Observer<T> { Unit }
        try {
            observeForever(observer)
            block()
        } finally {
            removeObserver(observer)
        }
    }

    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
    }
}