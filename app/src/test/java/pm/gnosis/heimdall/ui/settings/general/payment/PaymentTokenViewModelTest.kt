package pm.gnosis.heimdall.ui.settings.general.payment

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class PaymentTokenViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: PaymentTokenViewModel

    @Before
    fun setUp() {
        viewModel = PaymentTokenViewModel(context, tokenRepositoryMock)
    }

    @Test
    fun loadPaymentTokens() {
        val tokens = listOf(ERC20Token.ETHER_TOKEN, TEST_TOKEN)
        given(tokenRepositoryMock.loadPaymentTokens()).willReturn(Single.just(tokens))
        val observer = TestObserver<Adapter.Data<ERC20Token>>()
        viewModel.loadPaymentTokens().subscribe(observer)
        observer.assertSubscribed().assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue { it.entries == tokens && it.diff == null && it.parentId == null }
        then(context).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).should().loadPaymentTokens()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadPaymentTokensError() {
        context.mockGetString()
        given(tokenRepositoryMock.loadPaymentTokens()).willReturn(Single.error(UnknownHostException()))
        val observer = TestObserver<Adapter.Data<ERC20Token>>()
        viewModel.loadPaymentTokens().subscribe(observer)
        observer.assertSubscribed().assertNoValues()
            .assertError { it is SimpleLocalizedException && it.localizedMessage == R.string.error_check_internet_connection.toString() }
        then(context).should().getString(R.string.error_check_internet_connection)
        then(context).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadPaymentTokens()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun setPaymentToken() {
        given(tokenRepositoryMock.setPaymentToken(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())
        val observer = TestObserver<Result<ERC20Token>>()
        viewModel.setPaymentToken(TEST_TOKEN).subscribe(observer)
        observer.assertResult(DataResult(TEST_TOKEN))
        then(context).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).should().setPaymentToken(null, TEST_TOKEN)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun setPaymentTokenError() {
        val error = IllegalArgumentException()
        given(tokenRepositoryMock.setPaymentToken(MockUtils.any(), MockUtils.any())).willReturn(Completable.error(error))
        val observer = TestObserver<Result<ERC20Token>>()
        viewModel.setPaymentToken(TEST_TOKEN).subscribe(observer)
        observer.assertResult(ErrorResult(error))
        then(context).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).should().setPaymentToken(null, TEST_TOKEN)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_TOKEN = ERC20Token("0xdeadbeef".asEthereumAddress()!!, "DeadBeef", "DB", 18)
    }
}
