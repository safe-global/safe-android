package pm.gnosis.android.app.authenticator.data.model

import java.math.BigDecimal
import java.math.BigInteger

data class Wei(val value: BigInteger) {
    fun toEther(): BigDecimal = BigDecimal(value).setScale(3).div(BigDecimal(10).pow(18))
}
