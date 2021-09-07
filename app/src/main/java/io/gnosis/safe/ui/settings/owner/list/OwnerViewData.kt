package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.models.Owner
import io.gnosis.safe.R
import pm.gnosis.model.Solidity

data class OwnerViewData(
    val address: Solidity.Address,
    val name: String?,
    val type: Owner.Type
)

fun Owner.Type.imageRes16dp() = when (this) {
    Owner.Type.IMPORTED -> R.drawable.ic_key_type_imported_16dp
    Owner.Type.GENERATED -> R.drawable.ic_key_type_generated_16dp
    Owner.Type.LEDGER_NANO_X -> R.drawable.ic_key_type_ledger_16dp
}

fun Owner.Type.imageRes24dp() = when (this) {
    Owner.Type.IMPORTED -> R.drawable.ic_key_type_imported_24dp
    Owner.Type.GENERATED -> R.drawable.ic_key_type_generated_24dp
    Owner.Type.LEDGER_NANO_X -> R.drawable.ic_key_type_ledger_24dp
}

//    val imageRes36dp: Int = when (this) {
//            Owner.Type.IMPORTED -> R.drawable.ic_key_type_imported_36dp
//            Owner.Type.GENERATED -> R.drawable.ic_key_type_generated_36dp
//            Owner.Type.LEDGER_NANO_X -> R.drawable.ic_key_type_ledger_36dp
//        }
