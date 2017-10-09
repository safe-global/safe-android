package pm.gnosis.heimdall.ui.exceptions

import android.content.Context
import io.reactivex.observers.TestObserver
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.utils.HttpCodes
import retrofit2.HttpException
import retrofit2.Response
import java.lang.RuntimeException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.mockito.Mockito.`when` as given

@RunWith(MockitoJUnitRunner::class)
class LocalizedExceptionTest {

    @Mock
    lateinit var context: Context

    val handler: LocalizedException.Handler by lazy { LocalizedException.networkErrorHandlerBuilder(context).build(false) }

    @Test
    fun handleSSLError() {
        mockString(R.string.error_ssl_handshake, "error_ssl_handshake")
        check(LocalizedException("error_ssl_handshake"), SSLHandshakeException(""))
        check(LocalizedException("error_ssl_handshake"), RuntimeException(SSLHandshakeException("")))
    }

    @Test
    fun handleNoInternetError() {
        mockString(R.string.error_check_internet_connection, "error_check_internet_connection")
        check(LocalizedException("error_check_internet_connection"), UnknownHostException(""))
        check(LocalizedException("error_check_internet_connection"), SocketTimeoutException(""))
    }

    @Test
    fun handleUnauthorizedError() {
        mockString(R.string.error_not_authorized_for_action, "error_not_authorized_for_action")
        check(LocalizedException("error_not_authorized_for_action"), mockHttpException(HttpCodes.UNAUTHORIZED))
        check(LocalizedException("error_not_authorized_for_action"), mockHttpException(HttpCodes.FORBIDDEN))
    }

    @Test
    fun handleUnexpectedError() {
        mockString(R.string.error_try_again, "error_try_again")
        check(LocalizedException("error_try_again"), mockHttpException(HttpCodes.SERVER_ERROR))
        check(LocalizedException("error_try_again"), mockHttpException(HttpCodes.BAD_REQUEST))
        check(LocalizedException("error_try_again"), mockHttpException(HttpCodes.NOT_FOUND))
    }

    @Test
    fun doNotHandleUnknown() {
        val exception = IllegalStateException()
        check(exception, exception)
        verifyNoMoreInteractions(context)
    }

    private fun check(expected: Throwable, input: Throwable) {
        val observer = TestObserver.create<Any>()
        assertEquals(expected, handler.translate(input))
        handler.observable<Any>(input).subscribe(observer)
        observer.assertNoValues().assertError(expected)
    }

    private fun mockHttpException(statusCode: Int): HttpException {
        return HttpException(Response.error<Any>(statusCode, mock(ResponseBody::class.java)))
    }

    private fun mockString(stringRes: Int, content: String) {
        given(context.getString(eq(stringRes))).thenReturn(content)
    }
}