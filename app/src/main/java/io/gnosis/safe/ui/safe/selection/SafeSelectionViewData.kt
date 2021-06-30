package io.gnosis.safe.ui.safe.selection

import io.gnosis.data.models.Safe

sealed class SafeSelectionViewData {

    object AddSafeHeader : SafeSelectionViewData()

    data class ChainHeader(
        val name: String,
        val color: String
    ) : SafeSelectionViewData()

    data class SafeItem(
        val safe: Safe
    ) : SafeSelectionViewData()
}
