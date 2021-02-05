package io.gnosis.safe

import io.gnosis.safe.helpers.Offline
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ErrorHandlerTest {
    private val jsonContentType = "application/json".toMediaTypeOrNull()
    private val emptyResponseBody = "".toResponseBody(jsonContentType)

    @Test
    fun `toError() (Offline) should create Error101`() {

        val result = Offline().toError()

        assertTrue(result is Error.Error101)
        assertEquals(result.httpCode, 101)
    }
    @Test
    fun `toError() (HttpException with code 400 and no body) should create Error400 with http code 400`() {
        val exception = HttpException(Response.error<String>(400, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error400)
        assertEquals(result.httpCode, 400)
    }

    @Test
    fun `toError() (HttpException with code 401 and no body) should create Error401 with http code 401`() {
        val exception = HttpException(Response.error<String>(401, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error401)
        assertEquals(result.httpCode, 401)
    }

    @Test
    fun `toError() (HttpException with code 403 and no body) should create Error403 with http code 403`() {
        val exception = HttpException(Response.error<String>(403, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error403)
        assertEquals(result.httpCode, 403)
    }

    @Test
    fun `toError() (HttpException with code 404 and no body) should create Error404 with http code 404`() {
        val exception = HttpException(Response.error<String>(404, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error404)
        assertEquals(result.httpCode, 404)
    }

    @Test
    fun `toError() (HttpException with code 422 and no body) should create 42200 error`() {
        val exception = HttpException(Response.error<String>(422, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error42200)
        assertEquals(result.httpCode, 42200)
    }

    @Test
    fun `toError() (HttpException with code 422 and code 1 in body) should create 42201 error`() {
        val exception = HttpException(Response.error<String>(422, "{ \"code\": 1}".toResponseBody(jsonContentType)))

        val result = exception.toError()

        assertTrue(result is Error.Error42201)
        assertEquals(42201, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 422 and code 2 in body) should create 422xx error`() {
        val exception = HttpException(Response.error<String>(422, "{ \"code\": 2}".toResponseBody(jsonContentType)))

        val result = exception.toError()

        assertTrue(result is Error.Error422xx)
        assertEquals(42202, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 422 and code 9 in body) should create 422xx error`() {
        val exception = HttpException(Response.error<String>(422, "{ \"code\": 9}".toResponseBody(jsonContentType)))

        val result = exception.toError()

        assertTrue(result is Error.Error422xx)
        assertEquals(42209, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 422 and code 10 in body) should create 422xx error`() {
        val exception = HttpException(Response.error<String>(422, "{ \"code\": 10}".toResponseBody(jsonContentType)))

        val result = exception.toError()

        assertTrue(result is Error.Error422xx)
        assertEquals(42210, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 422 and code 35 in body) should create 422xx error with code 42235`() {
        val exception = HttpException(Response.error<String>(422, "{ \"code\": 35}".toResponseBody(jsonContentType)))

        val result = exception.toError()

        assertTrue(result is Error.Error422xx)
        assertEquals(42235, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 422 and code 50 in body) should create 42250 error`() {
        val exception = HttpException(Response.error<String>(422, "{ \"code\": 50}".toResponseBody(jsonContentType)))

        val result = exception.toError()

        assertTrue(result is Error.Error42250)
        assertEquals(42250, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 422 and code 99 in body) should create 422xx error with code 42299`() {
        val exception = HttpException(Response.error<String>(422, "{ \"code\": 99}".toResponseBody(jsonContentType)))

        val result = exception.toError()

        assertTrue(result is Error.Error422xx)
        assertEquals(42299, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 422 and code 100 in body) should create 422xx error with code 422100`() {
        val exception = HttpException(Response.error<String>(422, "{ \"code\": 100}".toResponseBody(jsonContentType)))

        val result = exception.toError()

        assertTrue(result is Error.Error422xx)
        assertEquals(422100, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 430) should create Error500 with code 430`() {
        val exception = HttpException(Response.error<String>(430, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error500)
        assertEquals(result.httpCode, 430)
    }

    @Test
    fun `toError() (HttpException with code 520) should create Error500 with code 520`() {
        val exception = HttpException(Response.error<String>(520, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error500)
        assertEquals(result.httpCode, 520)
    }

    @Test
    fun `toError() (HttpException with code 599) should create Error500 with code 599`() {
        val exception = HttpException(Response.error<String>(599, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error500)
        assertEquals(result.httpCode, 599)
    }

    @Test
    fun `toError() (HttpException with code 600) should create ErrorUnknown`() {
        val exception = HttpException(Response.error<String>(600, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.ErrorUnknown)
    }

    @Test
    fun `toError() (SSLHandshakeException) should create Error102`() {
        val exception = SSLHandshakeException("Fnord")

        val result = exception.toError()

        assertTrue(result is Error.Error102)
        assertEquals(result.httpCode, 102)
    }

    @Test
    fun `toError() (SocketTimeoutException) should create Error103`() {

        val result = SocketTimeoutException().toError()

        assertTrue(result is Error.Error103)
        assertEquals(result.httpCode, 103)
    }

    @Test
    fun `toError() (UnknownHostException) should create Error104`() {

        val result = UnknownHostException().toError()

        assertTrue(result is Error.Error104)
        assertEquals(result.httpCode, 104)
    }
}
