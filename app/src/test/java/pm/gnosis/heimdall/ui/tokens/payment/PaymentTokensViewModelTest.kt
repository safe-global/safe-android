package pm.gnosis.heimdall.ui.tokens.payment

import android.content.Context
import io.reactivex.Completable
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Before
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
class PaymentTokensViewModelTest{

    lateinit var paymentTokensViewModel:PaymentTokensViewModel

    @Mock
    @Before
    fun init(){
        paymentTokensViewModel = PaymentTokensViewModel(context,dispatchers,tokenRepository )
    }

    @Test
    fun test_SendPaymentToken(){


        val token = ERC20Token.ETHER_TOKEN

        given(tokenRepository.setPaymentToken(TEST_SAFE, token)).willReturn(Completable.complete())

        paymentTokensViewModel.paymentToken.observeForever(                                                                                                                               )
        paymentTokensViewModel.setup(TEST_SAFE, PaymentTokensContract.MetricType.Balance)
        paymentTokensViewModel.setPaymentToken(token)

        assertEquals(token,paymentTokensViewModel.paymentToken.value)


    }


    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
    }
}