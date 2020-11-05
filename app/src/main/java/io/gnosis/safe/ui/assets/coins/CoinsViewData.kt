package io.gnosis.safe.ui.assets.coins


sealed class CoinsViewData {

    data class CoinBalance(
        val symbol: String,
        val logoUri: String?,
        val balance: String,
        val balanceFiat: String,
        val balanceFiatStringRes: Int
    ) : CoinsViewData()

    data class TotalBalance(
        val totalFiat: String,
        val balanceFiatStringRes: Int
    ) : CoinsViewData()
}
