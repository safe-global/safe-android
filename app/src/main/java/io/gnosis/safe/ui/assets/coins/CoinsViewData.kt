package io.gnosis.safe.ui.assets.coins


sealed class CoinsViewData {

    data class CoinBalance(
        val symbol: String,
        val logoUri: String?,
        val balance: String,
        val balanceFiat: String
    ) : CoinsViewData()

    data class TotalBalance(
        val totalFiat: String
    ) : CoinsViewData()

    data class Banner(
        val type: Type
    ) : CoinsViewData() {
        enum class Type {
            IMPORT_OWNER_KEY,
            PASSCODE
        }
    }
}
