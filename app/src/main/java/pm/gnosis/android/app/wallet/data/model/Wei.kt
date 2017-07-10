package pm.gnosis.android.app.wallet.data.model

import java.math.BigDecimal
import java.math.BigInteger

data class Wei(val value: BigInteger) {
    fun toEther() = BigDecimal(value).div(BigDecimal("1000000000000000000")).setScale(18)
}