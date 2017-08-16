package pm.gnosis.android.app.authenticator.util

import java.math.BigInteger
import java.security.SecureRandom

fun generateRandomString(numBits: Int = 130, radix: Int = 32): String {
    return BigInteger(numBits, SecureRandom()).toString(radix)
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

fun String.hexToByteArray(): ByteArray {
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
