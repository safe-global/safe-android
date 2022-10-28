package io.gnosis.safe.ui.assets.coins

import java.io.Serializable
import java.math.BigDecimal


sealed class CoinsViewData : Serializable {

    data class CoinBalance(
        val address: String,
        val decimals: Int,
        val symbol: String,
        val logoUri: String?,
        val balance: BigDecimal,
        val balanceFormatted: String,
        val balanceFiatFormatted: String
    ) : CoinsViewData()

    data class TotalBalance(
        val totalFiat: String
    ) : CoinsViewData()

    data class Banner(
        val type: Type
    ) : CoinsViewData() {
        enum class Type {
            ADD_OWNER_KEY,
            PASSCODE,
            NONE
        }
    }
}
