package io.gnosis.safe.utils

import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun BigInteger.convertAmount(decimals: Int): BigDecimal =
    BigDecimal(this).setScale(decimals).div(BigDecimal.TEN.pow(decimals))

fun BigInteger.shiftedString(decimals: Int, decimalsToDisplay: Int = 5, roundingMode: RoundingMode = RoundingMode.DOWN) =
    convertAmount(decimals).setScale(decimalsToDisplay, roundingMode).stringWithNoTrailingZeroes()
