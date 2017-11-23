package pm.gnosis.utils

import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigDecimal
import java.math.BigInteger

fun String.hexAsEthereumAddressOrNull() = nullOnThrow { this.hexAsEthereumAddress() }
fun String.hexAsEthereumAddress(): BigInteger {
    try {
        val bigInt = hexAsBigInteger()
        if (bigInt.isValidEthereumAddress()) return bigInt
        else throw InvalidAddressException(this)
    } catch (e: Exception) {
        throw InvalidAddressException(this)
    }
}

fun String.hexAsBigInteger() = BigInteger(this.removePrefix("0x"), 16)
fun String.hexAsBigIntegerOrNull() = nullOnThrow { this.hexAsBigInteger() }
fun String.decimalAsBigInteger() = BigInteger(this, 10)
fun String.decimalAsBigIntegerOrNull() = nullOnThrow { this.decimalAsBigInteger() }

fun ByteArray.asBigInteger() = BigInteger(1, this)

fun BigInteger.asEthereumAddressStringOrNull() = nullOnThrow { this.asEthereumAddressString() }
fun BigInteger.asEthereumAddressString(): String {
    if (!isValidEthereumAddress()) throw InvalidAddressException(this)
    return "0x${this.toString(16).padStart(40, '0')}"
}

fun BigInteger.isValidEthereumAddress() = this <= BigInteger.valueOf(2).pow(160).minus(BigInteger.ONE)

fun BigInteger.asTransactionHash(): String {
    if (!isValidTransactionHash()) throw InvalidAddressException(this)
    return "0x${this.toString(16).padStart(64, '0')}"
}

fun BigInteger.isValidTransactionHash() = this <= BigInteger.valueOf(2).pow(256).minus(BigInteger.ONE)

fun BigInteger.asDecimalString(): String = this.toString(10)
fun BigDecimal.withTokenScaleOrNull(decimals: Int) = nullOnThrow { withTokenScale(decimals) }
fun BigDecimal.withTokenScale(decimals: Int) = this.setScale(decimals).div(BigDecimal.TEN.pow(decimals))
fun BigDecimal.stringWithNoTrailingZeroes(): String =
        //Issue: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6480539
        if (this.unscaledValue() == BigInteger.ZERO) "0"
        else this.stripTrailingZeros().toPlainString()

fun String.isValidEthereumAddress() = this.removePrefix("0x").length == 40 &&
        nullOnThrow { this.hexAsBigInteger() } != null
