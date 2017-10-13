package pm.gnosis.heimdall.data.repositories.model

import pm.gnosis.heimdall.data.model.Wei


data class MultisigWalletInfo(
        val address: String,
        val balance: Wei,
        val requiredConfirmations: Long,
        val owners: List<String>
)