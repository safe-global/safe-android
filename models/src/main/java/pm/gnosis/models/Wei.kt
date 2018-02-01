package pm.gnosis.models

import java.math.BigDecimal
import java.math.BigInteger

data class Wei(val value: BigInteger) {
    fun toEther(scale: Int = 18): BigDecimal = BigDecimal(value).setScale(scale).div(WEI_TO_ETHER_MULTIPLIER)

    fun toLong() = value.toLong()

    operator fun compareTo(other: Wei) = this.value.compareTo(other.value)

    companion object {
        private val WEI_TO_ETHER_MULTIPLIER = BigDecimal(10).pow(18)
        val ZERO = Wei(BigInteger.ZERO)
        fun ether(value: Double) = Wei((BigDecimal.valueOf(value) * WEI_TO_ETHER_MULTIPLIER).toBigInteger())
    }
}
