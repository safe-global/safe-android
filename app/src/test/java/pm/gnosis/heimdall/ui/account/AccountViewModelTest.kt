package pm.gnosis.heimdall.ui.account

import android.arch.persistence.room.EmptyResultSetException
import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.models.Currency
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigDecimal
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AccountViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    private lateinit var accountRepositoryMock: AccountsRepository

    @Mock
    private lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    private lateinit var qrCodeGeneratorMock: QrCodeGenerator

    @Mock
    private lateinit var tickerRepositoryMock: TickerRepository

    lateinit var viewModel: AccountViewModel

    @Before
    fun setup() {
        viewModel = AccountViewModel(
            contextMock, accountRepositoryMock, ethereumRepositoryMock,
            qrCodeGeneratorMock, tickerRepositoryMock
        )
        given(contextMock.getString(Mockito.anyInt())).willReturn(TEST_STRING)
    }

    @Test
    fun getQrCodeSuccess() {
        val observer = createObserver<Bitmap>()
        val bitmap = mock(Bitmap::class.java)
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.just(bitmap))

        viewModel.getQrCode("DATA").subscribe(observer)

        then(qrCodeGeneratorMock).should().generateQrCode("DATA")
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue { it is DataResult && it.data == bitmap }
    }

    @Test
    fun getQrCodeFailure() {
        val observer = createObserver<Bitmap>()
        val exception = IllegalStateException()
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.error(exception))

        viewModel.getQrCode("DATA").subscribe(observer)

        then(qrCodeGeneratorMock).should().generateQrCode("DATA")
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue { it is ErrorResult && it.error == exception }
    }

    @Test
    fun getAccountAddressNotAvailable() {
        val observer = createObserver<Account>()
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.error<Account>(EmptyResultSetException("")))

        viewModel.getAccountAddress().subscribe(observer)

        then(accountRepositoryMock).should().loadActiveAccount()
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue(ErrorResult(SimpleLocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.no_account_available)
    }

    @Test
    fun getAccountAddressError() {
        val observer = createObserver<Account>()
        val exception = IllegalStateException()
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.error<Account>(exception))

        viewModel.getAccountAddress().subscribe(observer)

        then(accountRepositoryMock).should().loadActiveAccount()
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        observer.assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue(ErrorResult(exception))
    }

    @Test
    fun getAccountAddressSuccess() {
        val observer = createObserver<Account>()
        val account = Account(Solidity.Address(BigInteger.ZERO))
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))

        viewModel.getAccountAddress().subscribe(observer)

        then(contextMock).shouldHaveZeroInteractions()
        then(accountRepositoryMock).should().loadActiveAccount()
        observer.assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue(DataResult(account))
    }

    @Test
    fun getAccountBalanceError() {
        val observer = createObserver<Wei>()
        val account = Account(Solidity.Address(BigInteger.ZERO))
        val exception = IllegalStateException()
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.error<Wei>(exception))

        viewModel.getAccountBalance().subscribe(observer)

        then(contextMock).shouldHaveZeroInteractions()
        then(accountRepositoryMock).should().loadActiveAccount()
        then(ethereumRepositoryMock).should().getBalance(account.address)
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue(ErrorResult(exception))
    }

    @Test
    fun getAccountBalanceNetworkError() {
        val observer = createObserver<Wei>()
        val account = Account(Solidity.Address(BigInteger.ZERO))
        val response = Response.error<Any>(401, mock(ResponseBody::class.java))
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.error<Wei>(HttpException(response)))

        viewModel.getAccountBalance().subscribe(observer)

        then(contextMock).should().getString(R.string.error_not_authorized_for_action)
        then(accountRepositoryMock).should().loadActiveAccount()
        then(ethereumRepositoryMock).should().getBalance(account.address)
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue(ErrorResult(SimpleLocalizedException(TEST_STRING)))
    }

    @Test
    fun getAccountBalanceSuccess() {
        val observer = createObserver<Wei>()
        val account = Account(Solidity.Address(BigInteger.ZERO))
        val balance = Wei(BigInteger.valueOf(1000))
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.getAccountBalance().subscribe(observer)

        then(contextMock).shouldHaveZeroInteractions()
        then(accountRepositoryMock).should().loadActiveAccount()
        then(ethereumRepositoryMock).should().getBalance(account.address)
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
            .assertValueCount(1)
            .assertValue(DataResult(balance))
    }

    @Test
    fun loadFiatConversion() {
        val amount = Wei(BigInteger.ZERO)
        val testObserver = TestObserver.create<Result<Pair<BigDecimal, Currency>>>()
        val currency = Currency("", "", "", 0, 0, BigDecimal.ZERO, Currency.FiatSymbol.USD)
        val fiatResult = BigDecimal.ZERO to currency
        given(tickerRepositoryMock.convertToFiat(MockUtils.any<Wei>(), MockUtils.any())).willReturn(Single.just(fiatResult))

        viewModel.loadFiatConversion(amount).subscribe(testObserver)

        then(tickerRepositoryMock).should().convertToFiat(amount)
        then(tickerRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(DataResult(fiatResult))
    }

    @Test
    fun loadFiatConversionError() {
        val amount = Wei(BigInteger.ZERO)
        val testObserver = TestObserver.create<Result<Pair<BigDecimal, Currency>>>()
        val exception = IllegalArgumentException()
        given(tickerRepositoryMock.convertToFiat(MockUtils.any<Wei>(), MockUtils.any())).willReturn(Single.error(exception))

        viewModel.loadFiatConversion(amount).subscribe(testObserver)

        then(tickerRepositoryMock).should().convertToFiat(amount)
        then(tickerRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    private fun <D> createObserver() = TestObserver.create<Result<D>>()

    companion object {
        const val TEST_STRING = "TEST"
    }
}
