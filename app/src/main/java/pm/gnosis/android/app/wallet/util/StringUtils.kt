package pm.gnosis.android.app.wallet.util

import java.math.BigInteger
import java.security.SecureRandom

fun generateRandomString(numBits: Int = 130, radix: Int = 32): String {
    return BigInteger(numBits, SecureRandom()).toString(radix)
}

fun String.hexToBigInteger(): BigInteger {
    return BigInteger(removePrefix("0x"), 16)
}
