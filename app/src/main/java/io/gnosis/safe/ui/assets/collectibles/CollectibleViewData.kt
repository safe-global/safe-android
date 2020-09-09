package io.gnosis.safe.ui.assets.collectibles

import io.gnosis.data.models.Collectible

sealed class CollectibleViewData {

    data class NftHeader(
        val tokenName: String,
        val contractLogoUri: String?,
        val first: Boolean = false
    ) : CollectibleViewData()

    data class CollectibleItem(
        val collectible: Collectible
    ) : CollectibleViewData()
}
