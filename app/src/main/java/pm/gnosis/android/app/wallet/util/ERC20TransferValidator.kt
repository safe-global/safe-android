package pm.gnosis.android.app.wallet.util

import java.math.BigDecimal
import java.math.BigInteger

class ERC20TransferValidator {
    companion object {
        const val TRANSFER_METHOD_ID = "a9059cbb" // first 4 bytes of the Keccak hash of transfer(address,uint256)
        const val DECIMAL_PLACES = 18 //TODO: retrieve this from contract

        fun parseTransfer(data: String): ERC20TokenTransfer? {
            if (data.startsWith("0x$TRANSFER_METHOD_ID")) {
                val arguments = data.removePrefix("0x$TRANSFER_METHOD_ID")
                if (arguments.length == 128) {
                    val to = arguments.substring(0, 64)
                    val value = arguments.substring(64, 128)
                    return ERC20TokenTransfer(to.hexAsBigInteger(), BigDecimal(value.hexAsBigInteger(), DECIMAL_PLACES))
                }
            }
            return null
        }
    }
}

data class ERC20TokenTransfer(val to: BigInteger, val value: BigDecimal)
