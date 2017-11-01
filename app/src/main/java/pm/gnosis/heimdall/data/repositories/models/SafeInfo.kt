package pm.gnosis.heimdall.data.repositories.models

import pm.gnosis.models.Wei


data class SafeInfo(
        val address: String,
        val balance: Wei,
        val requiredConfirmations: Long,
        val owners: List<String>
)