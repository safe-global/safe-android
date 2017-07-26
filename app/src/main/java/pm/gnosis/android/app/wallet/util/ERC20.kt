package pm.gnosis.android.app.wallet.util

import java.math.BigDecimal
import java.math.BigInteger

object ERC20 {
    const val DECIMAL_PLACES = 18 //TODO: retrieve this from contract
    const val DECIMALS_METHOD_ID = "0x313ce567"
    const val TRANSFER_METHOD_ID = "0xa9059cbb"
    const val SYMBOL_METHOD_ID = "0x95d89b41"
    const val NAME_METHOD_ID = "0x06fdde03"

    fun parseTransferData(data: String): ERC20TokenTransfer? {
        if (data.startsWith(TRANSFER_METHOD_ID)) {
            val arguments = data.removePrefix(TRANSFER_METHOD_ID)
            if (arguments.length == 128) {
                val to = arguments.substring(0, 64)
                val value = arguments.substring(64, 128)
                return ERC20TokenTransfer(to.hexAsBigInteger(), BigDecimal(value.hexAsBigInteger(), DECIMAL_PLACES))
            }
        }
        return null
    }
}

data class ERC20TokenTransfer(val to: BigInteger, val value: BigDecimal)
