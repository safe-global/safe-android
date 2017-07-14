package pm.gnosis.android.app.wallet.util

import java.math.BigInteger

fun String.hexAsBigInteger() = BigInteger(this.removePrefix("0x"), 16)
fun String.decimalAsBigInteger() = BigInteger(this, 10)

fun BigInteger.asHexString() = "0x${this.toString(16)}"
fun BigInteger.asDecimalString() = this.toString(10)