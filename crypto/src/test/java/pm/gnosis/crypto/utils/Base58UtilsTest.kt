package pm.gnosis.crypto.utils

import okio.ByteString
import org.junit.Test

import org.junit.Assert.*

class Base58UtilsTest {

    @Test
    fun encode() {
        assertEquals("", Base58Utils.encode(ByteString.encodeUtf8("")))
        assertEquals("JxF12TrwUP45BMd", Base58Utils.encode(ByteString.encodeUtf8("Hello World")))
    }

    @Test
    fun encodeChecked() {
        assertEquals("3QJmnh", Base58Utils.encodeChecked(ByteString.encodeUtf8("")))
        assertEquals("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM", Base58Utils.encodeChecked(
                toByteString(0x00, 0x01, 0x09, 0x66, 0x77, 0x60, 0x06, 0x95, 0x3D, 0x55, 0x67, 0x43, 0x9E, 0x5E, 0x39, 0xF8, 0x6A, 0x0D, 0x27, 0x3B, 0xEE)))
    }

    private fun toByteString(vararg inputs: Int) =
            ByteString.of(*inputs.map { it.toByte() }.toByteArray())
}