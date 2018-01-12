package pm.gnosis.heimdall.data.repositories.models

import pm.gnosis.models.Wei
import java.math.BigInteger

data class SafeInfo(
        val address: String,
        val balance: Wei,
        val requiredConfirmations: Long,
        val owners: List<BigInteger>,
        val isOwner: Boolean)
