package pm.gnosis.utils

import okio.ByteString
import java.math.BigInteger
import kotlin.experimental.and

private val hexArray = "0123456789abcdef".toCharArray()

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

/**
 * The regular {@link java.math.BigInteger#toByteArray()} method isn't quite what we often need:
 * it appends a leading zero to indicate that the number is positive and may need padding.
 *
 * @param b the integer to format into a byte array
 * @param numBytes the desired size of the resulting byte array
 * @return numBytes byte long array.
 */
fun BigInteger.toBytes(numBytes: Int): ByteArray {
    val bytes = kotlin.ByteArray(numBytes)
    val biBytes = toByteArray()
    val start = if (biBytes.size == numBytes + 1) 1 else 0
    val length = Math.min(biBytes.size, numBytes)
    System.arraycopy(biBytes, start, bytes, numBytes - length, length);
    return bytes
}


fun BigInteger.toAlfaNumericAscii(): String? {
    val hexString = this.toString(16)
    if (hexString.length.rem(2) != 0) return null
    val stringBuilder = StringBuilder()
    (0 until hexString.length step 2)
            .map { hexString.substring(it, it + 2) }
            .filter { it.toInt(16) in 32..126 } //alfanumeric
            .forEach { stringBuilder.append(it.toInt(16).toChar()) }

    return stringBuilder.toString()
}

fun String.hexStringToByteArray(): ByteArray {
    val s = this.removePrefix("0x")
    val len = s.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

fun String.addAddressPrefix() = if (!this.startsWith("0x")) "0x$this" else this

fun String.isSolidityMethod(methodId: String) = this.removePrefix("0x").startsWith(methodId.removePrefix("0x"))
fun String.removeSolidityMethodPrefix(methodId: String) = this.removePrefix("0x").removePrefix(methodId)
