package pm.gnosis.heimdall.utils

import android.content.Context
import pm.gnosis.heimdall.R
import pm.gnosis.models.Wei
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigInteger


fun Wei.displayString(context: Context) =
        if (value < BigInteger.TEN.pow(11)) {
            context.getString(R.string.x_wei, value.asDecimalString())!!
        } else {
            context.getString(R.string.x_ether, toEther().stringWithNoTrailingZeroes())!!
        }
