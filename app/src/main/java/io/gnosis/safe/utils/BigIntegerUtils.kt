package io.gnosis.safe.utils

import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun BigInteger.convertAmount(decimals: Int): BigDecimal =
    BigDecimal(this).setScale(decimals).div(BigDecimal.TEN.pow(decimals))

fun BigInteger.shifted(decimals: Int, decimalsToDisplay: Int = 5, roundingMode: RoundingMode = RoundingMode.DOWN): BigDecimal =
    convertAmount(decimals).setScale(decimalsToDisplay, roundingMode)

fun BigInteger.shiftedString(decimals: Int, decimalsToDisplay: Int = 5, roundingMode: RoundingMode = RoundingMode.DOWN) =
    shifted(decimals, decimalsToDisplay, roundingMode).stringWithNoTrailingZeroes()

fun BigInteger.formatAmount(incoming: Boolean, decimals: Int = 18, symbol: String = "ETH"): String {
    val inOut = if (this == BigInteger.ZERO) "" else if (incoming) "+" else "-"
    val decimalValue = this.shiftedString(decimals = decimals)
    return "%s%s %s".format(inOut, decimalValue, symbol)
}
