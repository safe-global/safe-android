package pm.gnosis.heimdall.utils

import android.content.Context
import pm.gnosis.heimdall.R
import pm.gnosis.models.Wei
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigInteger

fun Wei.displayString(context: Context) = when {
    value <= WEI_LIMIT ->
        context.getString(R.string.x_wei, value.asDecimalString())!!
    value <= GWEI_LIMIT ->
        context.getString(R.string.x_gwei, value.divide(GWEI_FACTOR).asDecimalString())!!
    else ->
        context.getString(R.string.x_ether, toEther().stringWithNoTrailingZeroes())!!
}

fun Wei.format(context: Context) = when {
    value <= WEI_LIMIT ->
        value.asDecimalString() to context.getString(R.string.currency_wei)
    value <= GWEI_LIMIT ->
        value.divide(GWEI_FACTOR).asDecimalString() to context.getString(R.string.currency_gwei)
    else ->
        toEther().stringWithNoTrailingZeroes() to context.getString(R.string.currency_eth)
}

private val WEI_LIMIT = BigInteger.TEN.pow(6)
private val GWEI_LIMIT = BigInteger.TEN.pow(14)
private val GWEI_FACTOR = BigInteger.TEN.pow(9)
