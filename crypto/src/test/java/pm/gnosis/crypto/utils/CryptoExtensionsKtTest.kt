package pm.gnosis.crypto.utils

import okio.ByteString
import org.junit.Test

import org.junit.Assert.*

class CryptoExtensionsKtTest {

    @Test
    fun rmd160() {
        assertEquals(ByteString.decodeHex("b472a266d0bd89c13706a4132ccfb16f7c3b9fcb"), ByteString.encodeUtf8("").hash160())
        val data = ByteString.encodeUtf8("Hello World")
        assertEquals(ByteString.decodeHex("bdfb69557966d026975bebe914692bf08490d8ca"), data.hash160())
    }
}