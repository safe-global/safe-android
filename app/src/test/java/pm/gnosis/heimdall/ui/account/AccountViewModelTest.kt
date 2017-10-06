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
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.util.DataResult
import pm.gnosis.heimdall.common.util.ErrorResult
import pm.gnosis.heimdall.common.util.QrCodeGenerator
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.security.SecurityViewModelTest
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigInteger
import org.mockito.Mockito.`when` as given

@RunWith(MockitoJUnitRunner::class)
class AccountViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var accountRepository: AccountsRepository

    @Mock
    lateinit var ethereumJsonRpcRepository: EthereumJsonRpcRepository

    @Mock
    lateinit var qrCodeGenerator: QrCodeGenerator

    @Before
    fun setup() {
        given(context.getString(Mockito.anyInt())).thenReturn(SecurityViewModelTest.TEST_STRING)
    }

    @Test
    fun getQrCodeSuccess() {
        val viewModel = createViewModel()
        val observer = createObserver<Bitmap>()

        val bitmap = mock(Bitmap::class.java)
        given(qrCodeGenerator.generateQrCode(anyString(), anyInt(), anyInt())).thenReturn(Observable.just(bitmap))

        viewModel.getQrCode("DATA").subscribe(observer)

        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue { it is DataResult && it.data == bitmap }
    }

    @Test
    fun getQrCodeFailure() {
        val viewModel = createViewModel()
        val observer = createObserver<Bitmap>()

        val exception = IllegalStateException()
        given(qrCodeGenerator.generateQrCode(anyString(), anyInt(), anyInt())).thenReturn(Observable.error(exception))

        viewModel.getQrCode("DATA").subscribe(observer)

        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue { it is ErrorResult && it.error == exception }
    }

    @Test
    fun getAccountAddressNotAvailable() {
        val viewModel = createViewModel()
        val observer = createObserver<Account>()

        given(accountRepository.loadActiveAccount()).thenReturn(Single.error<Account>(EmptyResultSetException("")))
        viewModel.getAccountAddress().subscribe(observer)

        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(ErrorResult(LocalizedException(TEST_STRING)))

        verify(context).getString(R.string.no_account_available)
    }

    @Test
    fun getAccountAddressError() {
        val viewModel = createViewModel()
        val observer = createObserver<Account>()

        val exception = IllegalStateException()
        given(accountRepository.loadActiveAccount()).thenReturn(Single.error<Account>(exception))
        viewModel.getAccountAddress().subscribe(observer)

        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(ErrorResult(exception))

        verifyNoMoreInteractions(context)
    }

    @Test
    fun getAccountAddressSuccess() {
        val viewModel = createViewModel()
        val observer = createObserver<Account>()

        val account = Account("00000000000000000000000000000000")
        given(accountRepository.loadActiveAccount()).thenReturn(Single.just(account))
        viewModel.getAccountAddress().subscribe(observer)

        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(DataResult(account))

        verifyNoMoreInteractions(context)
    }

    @Test
    fun getAccountBalanceError() {
        val viewModel = createViewModel()
        val observer = createObserver<Wei>()

        val exception = IllegalStateException()
        given(ethereumJsonRpcRepository.getBalance()).thenReturn(Observable.error<Wei>(exception))
        viewModel.getAccountBalance().subscribe(observer)

        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(ErrorResult(exception))
        verifyNoMoreInteractions(context)

    }

    @Test
    fun getAccountBalanceNetworkError() {
        val viewModel = createViewModel()
        val observer = createObserver<Wei>()
        val response = Response.error<Any>(401, mock(ResponseBody::class.java))

        given(ethereumJsonRpcRepository.getBalance()).thenReturn(Observable.error<Wei>(HttpException(response)))
        viewModel.getAccountBalance().subscribe(observer)

        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(ErrorResult(LocalizedException(TEST_STRING)))

        verify(context).getString(R.string.error_not_authorized_for_action)
    }

    @Test
    fun getAccountBalanceSuccess() {
        val viewModel = createViewModel()
        val observer = createObserver<Wei>()

        val balance = Wei(BigInteger.valueOf(1000))
        given(ethereumJsonRpcRepository.getBalance()).thenReturn(Observable.just(balance))
        viewModel.getAccountBalance().subscribe(observer)

        observer.assertNoErrors().assertComplete()
                .assertValueCount(1)
                .assertValue(DataResult(balance))
        verifyNoMoreInteractions(context)

    }

    private fun <D> createObserver() =
            TestObserver.create<Result<D>>()

    private fun createViewModel() =
            AccountViewModel(context, accountRepository, ethereumJsonRpcRepository, qrCodeGenerator)

    companion object {
        const val TEST_STRING = "TEST"
    }

}