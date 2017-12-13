package pm.gnosis.models

import java.math.BigDecimal
import java.math.BigInteger

data class Wei(val value: BigInteger) {
    fun toEther(): BigDecimal = BigDecimal(value).setScale(5).div(BigDecimal(10).pow(18))

    fun toLong() = value.toLong()

    companion object {
        val ZERO = Wei(BigInteger.ZERO)
    }
}
