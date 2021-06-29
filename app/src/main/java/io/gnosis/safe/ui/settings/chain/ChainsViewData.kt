package io.gnosis.safe.ui.settings.chain

import io.gnosis.data.models.Chain

sealed class ChainsViewData {

    data class ChainItem(
        val chain: Chain
    ) : ChainsViewData()

    object TitleItem : ChainsViewData()
}
