package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.models.Owner
import io.gnosis.safe.R
import pm.gnosis.model.Solidity

data class OwnerViewData(
    val address: Solidity.Address,
    val name: String?,
    val type: Owner.Type
)

fun OwnerViewData.getImageResForKeyType(): Int =
    when (type) {
        Owner.Type.IMPORTED -> R.drawable.ic_key_type_imported_16dp
        Owner.Type.GENERATED -> R.drawable.ic_key_type_generated_16dp
        Owner.Type.LEDGER_NANO_X -> R.drawable.ic_key_type_ledger_16dp
    }
