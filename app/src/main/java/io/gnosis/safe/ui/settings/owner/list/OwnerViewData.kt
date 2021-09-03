package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.models.OwnerType
import io.gnosis.safe.R
import pm.gnosis.model.Solidity

data class OwnerViewData(
    val address: Solidity.Address,
    val name: String?,
    val type: OwnerType
)

fun OwnerViewData.getImageResForOwnerType(): Int = type.getImageResForOwnerType()

fun OwnerType.getImageResForOwnerType(): Int =
    when (this) {
        OwnerType.IMPORTED -> R.drawable.ic_key_type_imported_16dp
        OwnerType.GENERATED -> R.drawable.ic_key_type_generated_16dp
        OwnerType.LEDGER_NANO_X -> R.drawable.ic_key_type_ledger_16dp
    }
