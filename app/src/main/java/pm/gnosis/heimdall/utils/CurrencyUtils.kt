package pm.gnosis.heimdall.utils

import android.content.Context
import pm.gnosis.heimdall.R
import pm.gnosis.models.Wei
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigInteger


fun Wei.displayString(context: Context) =
        if (value < BigInteger.TEN.pow(5)) {
            context.getString(R.string.x_wei, value.asDecimalString())!!
        } else if (value < BigInteger.TEN.pow(11)) {
            context.getString(R.string.x_gwei, value.divide(GWEI_FACTOR).asDecimalString())!!
        } else {
            context.getString(R.string.x_ether, toEther().stringWithNoTrailingZeroes())!!
        }

private val WEI_LIMIT = BigInteger.TEN.pow(11)
private val GWEI_LIMIT = BigInteger.TEN.pow(5)
private val GWEI_FACTOR = BigInteger.TEN.pow(9)
