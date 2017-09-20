package pm.gnosis.heimdall.util

import java.math.BigInteger

fun String.hexAsBigInteger() = BigInteger(this.removePrefix("0x"), 16)
fun String.hexAsBigIntegerOrNull() = nullOnThrow { this.hexAsBigInteger() }
fun String.decimalAsBigInteger() = BigInteger(this, 10)
fun String.decimalAsBigIntegerOrNull() = nullOnThrow { this.decimalAsBigInteger() }

fun BigInteger.asEthereumAddressString() = "0x${this.toString(16).padStart(40, '0')}"
fun BigInteger.asDecimalString() = this.toString(10)

fun String.isValidEthereumAddress() = this.removePrefix("0x").length == 40 &&
        nullOnThrow { this.hexAsBigInteger() } != null
