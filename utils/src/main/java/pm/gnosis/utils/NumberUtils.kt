package pm.gnosis.utils

import java.math.BigDecimal
import java.math.BigInteger

fun String.hexAsBigInteger() = BigInteger(this.removePrefix("0x"), 16)
fun String.hexAsBigIntegerOrNull() = nullOnThrow { this.hexAsBigInteger() }
fun String.decimalAsBigInteger() = BigInteger(this, 10)
fun String.decimalAsBigIntegerOrNull() = nullOnThrow { this.decimalAsBigInteger() }

fun ByteArray.asBigInteger() = BigInteger(1, this)

fun BigInteger.asEthereumAddressStringOrNull() = nullOnThrow { this.asEthereumAddressString() }
fun BigInteger.asEthereumAddressString(): String {
    if (!isValidEthereumAddress()) throw IllegalArgumentException("Invalid ethereum address")
    return "0x${this.toString(16).padStart(40, '0')}"
}

fun BigInteger.isValidEthereumAddress() = this <= BigInteger.valueOf(2).pow(160).minus(BigInteger.ONE)

fun BigInteger.asDecimalString() = this.toString(10)

fun BigDecimal.asNumberString() =
        if (this.compareTo(BigDecimal.ZERO) == 0) "0"
        else this.stripTrailingZeros().toPlainString()

fun String.isValidEthereumAddress() = this.removePrefix("0x").length == 40 &&
        nullOnThrow { this.hexAsBigInteger() } != null
