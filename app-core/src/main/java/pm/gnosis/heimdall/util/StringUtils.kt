package pm.gnosis.heimdall.util

import java.math.BigInteger
import java.security.SecureRandom

fun generateRandomString(numBits: Int = 130, radix: Int = 32): String {
    return BigInteger(numBits, SecureRandom()).toString(radix)
}

fun String.asEthereumAddressString() = "0x${this.padStart(40, '0')}"
