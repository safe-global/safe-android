package pm.gnosis.models

import java.math.BigDecimal
import java.math.BigInteger

data class Wei(val value: BigInteger) {
    fun toEther(scale: Int = 18): BigDecimal = BigDecimal(value).setScale(scale).div(BigDecimal(10).pow(18))

    fun toLong() = value.toLong()

    companion object {
        val ZERO = Wei(BigInteger.ZERO)
    }
}
