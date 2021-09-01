package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.models.Owner
import pm.gnosis.model.Solidity


sealed class OwnerViewData {

    data class LocalOwner(
        val address: Solidity.Address,
        val name: String?,
        val type: Owner.Type
    ) : OwnerViewData()
}
