package pm.gnosis.heimdall.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger

class GnoSafeUrlParserTest {

    private fun testSignRequest(value: Wei?, data: String?) {
        val request = GnoSafeUrlParser.signRequest(TEST_HASH, TEST_SAFE, TEST_TO, value, data, TEST_NONCE)
        val parsed = GnoSafeUrlParser.parse(request)
        val expectedTx = Transaction(TEST_TO, value, data = data, nonce = TEST_NONCE)
        assertEquals(GnoSafeUrlParser.Parsed.SignRequest(TEST_HASH, TEST_SAFE, expectedTx), parsed)
    }

    @Test
    fun signRequest() {
        testSignRequest(null, null)
        testSignRequest(null, "")
        testSignRequest(Wei.ZERO, null)
        testSignRequest(Wei.ZERO, "")
        testSignRequest(null, "0x313ce567")
        testSignRequest(Wei.ether("0.0001"), null)
        testSignRequest(Wei.ether("0.0001"), "0x313ce567")
    }

    @Test
    fun signResponse() {
        val signature = Signature(BigInteger.valueOf(456321), BigInteger.valueOf(987654), 27)
        val request = GnoSafeUrlParser.signResponse(signature)
        val parsed = GnoSafeUrlParser.parse(request)
        assertEquals(GnoSafeUrlParser.Parsed.SignResponse(signature), parsed)
    }

    @Test
    fun parseUnknown() {
        assertNull("No gnosafe scheme", GnoSafeUrlParser.parse("url_without_gnosafe_scheme://sign_res/000000000000000000000000000000000000000000000000000000000006f68100000000000000000000000000000000000000000000000000000000000f12061b"))
        assertNull("Invalid sign response", GnoSafeUrlParser.parse("gnosafe://sign_res/000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f12061b"))
        assertNull("Invalid sign request", GnoSafeUrlParser.parse("gnosafe://sign_req/000000000000000000000000000000000000000000000000000000000000f12061b"))
        assertNull("Unknown type", GnoSafeUrlParser.parse("gnosafe://unknown_type/000000000000000000000000000000000000000000000000000000000000f12061b"))
    }

    companion object {
        private const val TEST_HASH = "Some_Hash"
        private val TEST_SAFE = BigInteger.valueOf(456789)
        private val TEST_TO = BigInteger.valueOf(987654)
        private val TEST_NONCE = BigInteger.ZERO
    }
}