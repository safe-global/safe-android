package pm.gnosis.utils

import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.tests.utils.Asserts.assertThrow
import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigDecimal
import java.math.BigInteger

class NumberUtilsKtTest {

    @Test
    fun hexAsEthereumAddressOrNull() {
        assertNull("ffffffffffffffffffffffffffffffffffffffffff".hexAsEthereumAddressOrNull())
        assertNull("0xffffffffffffffffffffffffffffffffffffffffff".hexAsEthereumAddressOrNull())
        assertNull("thisisporbablynotahexnumber".hexAsEthereumAddressOrNull())
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16), "ffffffffffffffffffffffffffffffffffffffff".hexAsEthereumAddressOrNull())
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16), "0xffffffffffffffffffffffffffffffffffffffff".hexAsEthereumAddressOrNull())
        assertEquals(BigInteger("abcd", 16), "abcd".hexAsEthereumAddressOrNull())
        assertEquals(BigInteger("abcd", 16), "0xabcd".hexAsEthereumAddressOrNull())
    }

    @Test
    fun hexAsEthereumAddress() {
        assertThrow({ "ffffffffffffffffffffffffffffffffffffffffff".hexAsEthereumAddress() }, throwablePredicate = { it is InvalidAddressException })
        assertThrow({ "0xffffffffffffffffffffffffffffffffffffffffff".hexAsEthereumAddress() }, throwablePredicate = { it is InvalidAddressException })
        assertThrow({ "thisisporbablynotahexnumber".hexAsEthereumAddress() }, throwablePredicate = { it is InvalidAddressException })
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16), "ffffffffffffffffffffffffffffffffffffffff".hexAsEthereumAddress())
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16), "0xffffffffffffffffffffffffffffffffffffffff".hexAsEthereumAddress())
        assertEquals(BigInteger("abcd", 16), "abcd".hexAsEthereumAddress())
        assertEquals(BigInteger("abcd", 16), "0xabcd".hexAsEthereumAddress())
    }

    @Test
    fun isValidEthereumAddressString() {
        assertFalse("ffffffffffffffffffffffffffffffffffffffffff".isValidEthereumAddress())
        assertFalse("0xffffffffffffffffffffffffffffffffffffffffff".isValidEthereumAddress())
        assertFalse("thisisporbablynotahexnumber".isValidEthereumAddress())
        assertFalse("abcd".isValidEthereumAddress())
        assertFalse("0xabcd".isValidEthereumAddress())
        assertTrue("ffffffffffffffffffffffffffffffffffffffff".isValidEthereumAddress())
        assertTrue("0xffffffffffffffffffffffffffffffffffffffff".isValidEthereumAddress())
    }

    @Test
    fun hexAsBigInteger() {
        assertThrow({ "thisisporbablynotahexnumber".hexAsBigInteger() })
        assertEquals(
                BigInteger("ffffffffffffffffffffffffffffffffffffffffff", 16),
                "ffffffffffffffffffffffffffffffffffffffffff".hexAsBigInteger())
        assertEquals(
                BigInteger("ffffffffffffffffffffffffffffffffffffffffff", 16),
                "0xffffffffffffffffffffffffffffffffffffffffff".hexAsBigInteger())
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16), "ffffffffffffffffffffffffffffffffffffffff".hexAsBigInteger())
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16), "0xffffffffffffffffffffffffffffffffffffffff".hexAsBigInteger())
        assertEquals(BigInteger("abcd", 16), "abcd".hexAsBigInteger())
        assertEquals(BigInteger("abcd", 16), "0xabcd".hexAsBigInteger())
    }

    @Test
    fun hexAsBigIntegerOrNull() {
        assertNull("thisisporbablynotahexnumber".hexAsBigIntegerOrNull())
        assertEquals(
                BigInteger("ffffffffffffffffffffffffffffffffffffffffff", 16),
                "ffffffffffffffffffffffffffffffffffffffffff".hexAsBigIntegerOrNull())
        assertEquals(
                BigInteger("ffffffffffffffffffffffffffffffffffffffffff", 16),
                "0xffffffffffffffffffffffffffffffffffffffffff".hexAsBigIntegerOrNull())
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16), "ffffffffffffffffffffffffffffffffffffffff".hexAsBigIntegerOrNull())
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16), "0xffffffffffffffffffffffffffffffffffffffff".hexAsBigIntegerOrNull())
        assertEquals(BigInteger("abcd", 16), "abcd".hexAsBigIntegerOrNull())
        assertEquals(BigInteger("abcd", 16), "0xabcd".hexAsBigIntegerOrNull())
    }

    @Test
    fun decimalAsBigInteger() {
        assertThrow({ "thisisporbablynotadecimalnumber".decimalAsBigInteger() })
        assertThrow({ "ffffffffffffffffffffffffffffffffffffffffff".decimalAsBigInteger() })
        assertEquals(BigInteger.valueOf(123456), "123456".decimalAsBigInteger())
        assertEquals(BigInteger("123456789101112131415161718192021222324252627282930"), "123456789101112131415161718192021222324252627282930".decimalAsBigInteger())
    }

    @Test
    fun decimalAsBigIntegerOrNull() {
        assertNull("thisisporbablynotadecimalnumber".decimalAsBigIntegerOrNull())
        assertNull("ffffffffffffffffffffffffffffffffffffffffff".decimalAsBigIntegerOrNull())
        assertEquals(BigInteger.valueOf(123456), "123456".decimalAsBigIntegerOrNull())
        assertEquals(BigInteger("123456789101112131415161718192021222324252627282930"), "123456789101112131415161718192021222324252627282930".decimalAsBigIntegerOrNull())
    }

    @Test
    fun asBigInteger() {
        val source = byteArrayOf(0x3a, 0x2b, 0x1c)
        assertEquals(BigInteger(source), source.asBigInteger())
    }

    @Test
    fun asEthereumAddressStringOrNull() {
        assertNull("0x0000000000000000000000000000000000000001", BigInteger("ffffffffffffffffffffffffffffffffffffffffff", 16).asEthereumAddressStringOrNull())
        assertEquals("0x0000000000000000000000000000000000000001", BigInteger.ONE.asEthereumAddressStringOrNull())
        assertEquals("0xffffffffffffffffffffffffffffffffffffffff", BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16).asEthereumAddressStringOrNull())
        assertEquals("0x000000000000000000000000000000000000abcd", BigInteger("abcd", 16).asEthereumAddressStringOrNull())
    }

    @Test
    fun asEthereumAddressString() {
        assertThrow({ BigInteger("ffffffffffffffffffffffffffffffffffffffffff", 16).asEthereumAddressString() })
        assertEquals("0x0000000000000000000000000000000000000001", BigInteger.ONE.asEthereumAddressString())
        assertEquals("0xffffffffffffffffffffffffffffffffffffffff", BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16).asEthereumAddressString())
        assertEquals("0x000000000000000000000000000000000000abcd", BigInteger("abcd", 16).asEthereumAddressString())
    }

    @Test
    fun isValidEthereumAddress() {
        assertFalse(BigInteger("ffffffffffffffffffffffffffffffffffffffffff", 16).isValidEthereumAddress())
        assertTrue(BigInteger.ONE.isValidEthereumAddress())
        assertTrue(BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16).isValidEthereumAddress())
        assertTrue(BigInteger("abcd", 16).isValidEthereumAddress())
    }

    @Test
    fun asTransactionHash() {
        assertThrow({ BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16).asTransactionHash() })
        assertEquals("0x0000000000000000000000000000000000000000000000000000000000000001", BigInteger.ONE.asTransactionHash())
        assertEquals("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16).asTransactionHash())
        assertEquals("0x000000000000000000000000000000000000000000000000000000000000abcd", BigInteger("abcd", 16).asTransactionHash())
    }

    @Test
    fun isValidTransactionHash() {
        assertFalse(BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16).isValidTransactionHash())
        assertTrue(BigInteger.ONE.isValidTransactionHash())
        assertTrue(BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16).isValidTransactionHash())
        assertTrue(BigInteger("abcd", 16).isValidTransactionHash())
    }

    @Test
    fun asDecimalString() {
        assertEquals("0", BigInteger.ZERO.asDecimalString())
        assertEquals("123456", BigInteger.valueOf(123456).asDecimalString())
        assertEquals("123456789101112131415161718192021222324252627282930", BigInteger("123456789101112131415161718192021222324252627282930").asDecimalString())
    }

    @Test
    fun stringWithNoTrailingZeroes() {
        assertEquals("0", BigDecimal.valueOf(0.0).setScale(6).stringWithNoTrailingZeroes())
        assertEquals("1.4", BigDecimal.valueOf(1.4).setScale(6).stringWithNoTrailingZeroes())
    }
}
