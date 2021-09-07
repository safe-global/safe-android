package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.models.Owner.Type
import io.gnosis.safe.R
import pm.gnosis.model.Solidity

data class OwnerViewData(
    val address: Solidity.Address,
    val name: String?,
    val type: Type
)

fun OwnerViewData.getImageRes(): Int = type.getImageRes()

fun Type.getImageRes(): Int =
    when (this) {
        Type.IMPORTED -> R.drawable.ic_key_type_imported_16dp
        Type.GENERATED -> R.drawable.ic_key_type_generated_16dp
        Type.LEDGER_NANO_X -> R.drawable.ic_key_type_ledger_16dp
    }
