package pm.gnosis.heimdall.data.repositories.models

import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei

data class SafeInfo(
    val address: String,
    val balance: Wei,
    val requiredConfirmations: Long,
    val owners: List<Solidity.Address>,
    val isOwner: Boolean,
    val modules: List<Solidity.Address>
)
