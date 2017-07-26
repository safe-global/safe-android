package pm.gnosis.android.app.wallet.util

import java.math.BigInteger
import java.security.SecureRandom

fun generateRandomString(numBits: Int = 130, radix: Int = 32): String {
    return BigInteger(numBits, SecureRandom()).toString(radix)
}

fun BigInteger.toAscii(): String {
    val hexString = this.toString(16)
    val stringBuilder = StringBuilder()
    (0..hexString.length - 1 step 2)
            .map { hexString.substring(it, it + 2) }
            .filter { it.toInt(16) in 32..126 } //alfanumeric
            .forEach { stringBuilder.append(it.toInt(16).toChar()) }

    return stringBuilder.toString()
}
