package pm.gnosis.crypto.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.utils.hexStringToByteArray

class Sha3UtilsTest {
    @Test
    fun hashing() {
        val data = "Hello World".toByteArray()
        val expectedSha3 = "e167f68d6563d75bb25f3aa49c29ef612d41352dc00606de7cbd630bb2665f51"
        assertArrayEquals(expectedSha3.hexStringToByteArray(), Sha3Utils.sha3(data))
        assertEquals(expectedSha3, Sha3Utils.sha3String(data))
        assertArrayEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a".hexStringToByteArray(), Sha3Utils.sha3(ByteArray(0)))

        val expectedKeccak = "592fa743889fc7f92ac2a37bb1f5ba1daf2a5c84741ca0e0061d243a2e6707ba"
        assertArrayEquals(expectedKeccak.hexStringToByteArray(), Sha3Utils.keccak(data))
        assertArrayEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470".hexStringToByteArray(), Sha3Utils.keccak(ByteArray(0)))
    }
}
