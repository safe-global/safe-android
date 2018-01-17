package pm.gnosis.utils

import okio.ByteString
import org.junit.Test

import org.junit.Assert.*
import pm.gnosis.tests.utils.Asserts.assertThrow
import java.math.BigInteger

class ExtensionsKtTest {

    @Test
    fun testGenerateRandomString() {
        for (i in 0..1000) {
            assertNotEquals("Generated Strings should not be equal", generateRandomString(), generateRandomString())
        }
        for (i in 0..1000) {
            val random = generateRandomString(1, 16)
            assertTrue("$random should be 0 or 1", "1" == random || "0" == random)
        }
    }

    @Test
    fun testToHexString() {
        assertEquals("", byteArrayOf().toHexString())
        assertEquals("0f", byteArrayOf(15.toByte()).toHexString())
        assertEquals("10", byteArrayOf(16.toByte()).toHexString())
        assertEquals("00de23dc54f047bcbd66a12ded71adcf3f34369c35", BigInteger("de23dc54f047bcbd66a12ded71adcf3f34369c35", 16).toByteArray().toHexString())
    }

    @Test
    fun testBigInt() {
        val input = ByteString.decodeHex("de23dc54f047bcbd66a12ded71adcf3f34369c35")
        assertEquals(BigInteger("de23dc54f047bcbd66a12ded71adcf3f34369c35", 16), input.bigInt())
    }

    @Test
    fun testUtf8String() {
        assertEquals("Hello World", "Hello World".toByteArray().utf8String())
    }

    @Test
    fun testToBytes() {
        val source = BigInteger("aa13", 16)
        assertArrayEquals(byteArrayOf(0xaa.toByte(), 0x13.toByte()), source.toBytes(2))

        assertArrayEquals(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xaa.toByte(), 0x13.toByte()), source.toBytes(4))
    }

    @Test
    fun testHexStringToByteArrayOrNull() {
        // Invalid length
        assertEquals(null, "0aa".hexStringToByteArrayOrNull())
        // Invalid chars
        assertEquals(null, "0aat".hexStringToByteArrayOrNull())
        // Valid
        assertArrayEquals(byteArrayOf(0x0a, 0xac.toByte()), "0aac".hexStringToByteArrayOrNull()!!)
    }

    @Test
    fun testHexStringToByteArray() {
        assertThrow({ "0aa".hexStringToByteArray() }, "Invalid length")
        assertThrow({ "0aat".hexStringToByteArray() }, "Invalid char")
        assertArrayEquals(byteArrayOf(0x0a, 0xac.toByte()), "0aac".hexStringToByteArray())
        assertArrayEquals(byteArrayOf(0x0a, 0xac.toByte()), "0x0aac".hexStringToByteArray())
    }

    @Test
    fun testAddHexPrefix() {
        assertEquals("0x0abced", "0abced".addHexPrefix())
        assertEquals("0x0abced", "0x0abced".addHexPrefix())
        // Works for any string
        assertEquals("0xblablainvalidhex", "blablainvalidhex".addHexPrefix())
    }

    @Test
    fun testRemoveHexPrefix() {
        assertEquals("0abced", "0abced".removeHexPrefix())
        assertEquals("0abced", "0x0abced".removeHexPrefix())
        // Works for any string
        assertEquals("xblablainvalidhex", "xblablainvalidhex".removeHexPrefix())
    }

    @Test
    fun testAsEthereumAddressString() {
        assertThrow({ "ffffffffffffffffffffffffffffffffffffffffff".asEthereumAddressString() })
        assertThrow({ "0xffffffffffffffffffffffffffffffffffffffffff".asEthereumAddressString() })
        assertEquals("0xffffffffffffffffffffffffffffffffffffffff", "ffffffffffffffffffffffffffffffffffffffff".asEthereumAddressString())
        assertEquals("0xffffffffffffffffffffffffffffffffffffffff", "0xffffffffffffffffffffffffffffffffffffffff".asEthereumAddressString())
        assertEquals("0x000000000000000000000000000000000000abcd", "abcd".asEthereumAddressString())
        assertEquals("0x000000000000000000000000000000000000abcd", "0xabcd".asEthereumAddressString())
    }

    @Test
    fun testIsSolidityMethod() {
        assertTrue("Should be name method", "0x313ce567somerandomdata".isSolidityMethod(ERC_20_NAME_METHOD_ID))
        assertTrue("Should be name method", "313ce567somerandomdata".isSolidityMethod(ERC_20_NAME_METHOD_ID))
        assertTrue("Should be name method", "0x313ce567somerandomdata".isSolidityMethod("0x$ERC_20_NAME_METHOD_ID"))
        assertTrue("Should be name method", "313ce567somerandomdata".isSolidityMethod("0x$ERC_20_NAME_METHOD_ID"))
        assertFalse("Should not be name method", "0x313cf567somerandomdata".isSolidityMethod(ERC_20_NAME_METHOD_ID))
        assertFalse("Should not be name method", "313cef67somerandomdata".isSolidityMethod(ERC_20_NAME_METHOD_ID))
        assertFalse("Should not be name method", "0x31fce567somerandomdata".isSolidityMethod("0x$ERC_20_NAME_METHOD_ID"))
        assertFalse("Should not be name method", "313cef67somerandomdata".isSolidityMethod("0x$ERC_20_NAME_METHOD_ID"))
    }

    @Test
    fun testRemoveSolidityMethodPrefix() {
        assertEquals("somerandomdata", "0x313ce567somerandomdata".removeSolidityMethodPrefix(ERC_20_NAME_METHOD_ID))
        assertEquals("somerandomdata", "313ce567somerandomdata".removeSolidityMethodPrefix(ERC_20_NAME_METHOD_ID))
        assertEquals("somerandomdata", "0x313ce567somerandomdata".removeSolidityMethodPrefix("0x$ERC_20_NAME_METHOD_ID"))
        assertEquals("somerandomdata", "313ce567somerandomdata".removeSolidityMethodPrefix("0x$ERC_20_NAME_METHOD_ID"))
    }

    @Test
    fun testToBinaryString() {
        val actual = "0123456789abcdef".hexStringToByteArray().toBinaryString()
        val expected = "0000000100100011010001010110011110001001101010111100110111101111"
        assertEquals(expected, actual)
    }

    @Test
    fun testGetIndexes() {
        val source = listOf("This", "is", "a", "test", "for", "getting", "all", "the", "indices")
        assertThrow({ source.getIndexes(listOf("is", "nope")) })
        assertArrayEquals(arrayOf(3, 0, 1, 7, 6), source.getIndexes(listOf("test", "This", "is", "the", "all")))
    }

    companion object {
        const val ERC_20_NAME_METHOD_ID = "313ce567"
    }
}
