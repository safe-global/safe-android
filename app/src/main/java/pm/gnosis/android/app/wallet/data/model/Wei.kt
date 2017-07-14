package pm.gnosis.android.app.wallet.data.model

import java.math.BigDecimal
import java.math.BigInteger

data class Wei(val value: BigInteger) {
    fun toEther(): BigDecimal = BigDecimal(value).div(BigDecimal(10).pow(18)).setScale(18)
}
