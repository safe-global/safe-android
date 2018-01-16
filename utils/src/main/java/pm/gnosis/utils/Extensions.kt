package pm.gnosis.utils

import okio.ByteString
import java.math.BigInteger
import java.security.SecureRandom
import java.util.regex.Pattern
import kotlin.experimental.and

private val hexArray = "0123456789abcdef".toCharArray()
private val hexPattern = Pattern.compile("(0[xX])?[0-9a-fA-F]+")
private const val HEX_PREFIX = "0x"

fun generateRandomString(numBits: Int = 130, radix: Int = 32): String {
    return BigInteger(numBits, SecureRandom()).toString(radix)
}

fun ByteArray.toHexString(): String {
    val hexChars = CharArray(this.size * 2)
    for (j in this.indices) {
        val v = ((this[j] and 0xFF.toByte()).toInt() + 256) % 256
        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}

fun ByteString.bigInt(): BigInteger {
    return BigInteger(1, toByteArray())
}

fun ByteArray.utf8String(): String {
    return String(this)
}

/**
 * The regular {@link java.math.BigInteger#toByteArray()} method isn't quite what we often need:
 * it appends a leading zero to indicate that the number is positive and may need padding.
 *
 * @param numBytes the desired size of the resulting byte array
 * @return numBytes byte long array.
 */
fun BigInteger.toBytes(numBytes: Int): ByteArray {
    val bytes = kotlin.ByteArray(numBytes)
    val biBytes = toByteArray()
    val start = if (biBytes.size == numBytes + 1) 1 else 0
    val length = Math.min(biBytes.size, numBytes)
    System.arraycopy(biBytes, start, bytes, numBytes - length, length)
    return bytes
}

fun String.hexStringToByteArrayOrNull() =
        nullOnThrow { hexStringToByteArray() }

fun String.hexStringToByteArray(): ByteArray {
    val s = this.removeHexPrefix()
    if (this.length % 2 != 0 || !hexPattern.matcher(this).matches()) {
        throw IllegalArgumentException("Invalid hex string")
    }
    val len = s.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

fun String.addHexPrefix() = if (!this.startsWith(HEX_PREFIX)) "$HEX_PREFIX$this" else this

fun String.removeHexPrefix() = this.removePrefix(HEX_PREFIX)

fun String.asEthereumAddressString(): String {
    val string = this.removeHexPrefix()
    val numericValue = BigInteger(string, 16)
    if (!numericValue.isValidEthereumAddress()) throw IllegalArgumentException("Invalid ethereum address")

    return numericValue.toString(16).padStart(40, '0').addHexPrefix()
}

fun String.isSolidityMethod(methodId: String) = this.removeHexPrefix().startsWith(methodId.removeHexPrefix())
fun String.removeSolidityMethodPrefix(methodId: String) = this.removeHexPrefix().removePrefix(methodId.removeHexPrefix())

fun ByteArray.toBinaryString(): String {
    val sb = StringBuilder(this.size * java.lang.Byte.SIZE)
    for (i in 0 until java.lang.Byte.SIZE * this.size) {
        sb.append(if (this[i / java.lang.Byte.SIZE].toInt() shl i % java.lang.Byte.SIZE and 0x80 == 0) '0' else '1')
    }
    return sb.toString()
}

/**
 * @return array of indexes for the specified collection.
 * @throws IllegalArgumentException if an item is not present
 */
@Throws(IllegalArgumentException::class)
fun <T> Collection<T>.getIndexes(items: List<T>): Array<Int> {
    if (items.isEmpty()) return emptyArray()

    return items.map {
        val index = this.indexOf(it)
        if (index == -1) throw IllegalArgumentException("$it is not present on the list")
        index
    }.toTypedArray()
}
