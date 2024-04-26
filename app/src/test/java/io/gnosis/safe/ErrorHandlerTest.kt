package io.gnosis.safe

import io.gnosis.safe.helpers.Offline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals(101, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 400 and no body) should create Error400 with http code 400`() {
        val exception = HttpException(Response.error<String>(400, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error400)
        assertEquals(400, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 401 and no body) should create Error401 with http code 401`() {
        val exception = HttpException(Response.error<String>(401, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error401)
        assertEquals(401, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 403 and no body) should create Error403 with http code 403`() {
        val exception = HttpException(Response.error<String>(403, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error403)
        assertEquals(403, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 404 and no body) should create Error404 with http code 404`() {
        val exception = HttpException(Response.error<String>(404, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error404)
        assertEquals(404, result.httpCode)
    }

    @Test
    fun `toError() (HttpException with code 422) should create 42200 error`() {
        val exception = HttpException(Response.error<String>(422, emptyResponseBody))

        val result = exception.toError()

        assertTrue(result is Error.Error42200)
        assertEquals(42200, result.httpCode)
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
