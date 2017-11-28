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
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.security.SecurityViewModelTest
import pm.gnosis.models.Wei
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AccountViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var accountRepositoryMock: AccountsRepository

    @Mock
    lateinit var ethereumJsonRpcRepositoryMock: EthereumJsonRpcRepository

    @Mock
    lateinit var qrCodeGeneratorMock: QrCodeGenerator

    lateinit var viewModel: AccountViewModel

    @Before
    fun setup() {
        viewModel = AccountViewModel(contextMock, accountRepositoryMock, ethereumJsonRpcRepositoryMock, qrCodeGeneratorMock)
        given(contextMock.getString(Mockito.anyInt())).willReturn(SecurityViewModelTest.TEST_STRING)
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
                .assertValue(ErrorResult(LocalizedException(TEST_STRING)))
        verify(contextMock).getString(R.string.no_account_available)
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
        val account = Account(BigInteger.ZERO)
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
        val account = Account(BigInteger.ZERO)
        val exception = IllegalStateException()
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumJsonRpcRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.error<Wei>(exception))

        viewModel.getAccountBalance().subscribe(observer)

        then(contextMock).shouldHaveZeroInteractions()
        then(accountRepositoryMock).should().loadActiveAccount()
        then(ethereumJsonRpcRepositoryMock).should().getBalance(account.address)
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumJsonRpcRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(ErrorResult(exception))
    }

    @Test
    fun getAccountBalanceNetworkError() {
        val observer = createObserver<Wei>()
        val account = Account(BigInteger.ZERO)
        val response = Response.error<Any>(401, mock(ResponseBody::class.java))
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumJsonRpcRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.error<Wei>(HttpException(response)))

        viewModel.getAccountBalance().subscribe(observer)

        then(contextMock).should().getString(R.string.error_not_authorized_for_action)
        then(accountRepositoryMock).should().loadActiveAccount()
        then(ethereumJsonRpcRepositoryMock).should().getBalance(account.address)
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumJsonRpcRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(ErrorResult(LocalizedException(TEST_STRING)))
    }

    @Test
    fun getAccountBalanceSuccess() {
        val observer = createObserver<Wei>()
        val account = Account(BigInteger.ZERO)
        val balance = Wei(BigInteger.valueOf(1000))
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumJsonRpcRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.getAccountBalance().subscribe(observer)

        then(contextMock).shouldHaveZeroInteractions()
        then(accountRepositoryMock).should().loadActiveAccount()
        then(ethereumJsonRpcRepositoryMock).should().getBalance(account.address)
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumJsonRpcRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(DataResult(balance))
    }

    private fun <D> createObserver() =
            TestObserver.create<Result<D>>()

    companion object {
        const val TEST_STRING = "TEST"
    }
}
