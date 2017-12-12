package pm.gnosis.heimdall.data.repositories.models

import java.math.BigInteger

sealed class AbstractSafe

data class Safe(val address: BigInteger, val name: String? = null) : AbstractSafe()

data class PendingSafe(val hash: BigInteger, val name: String? = null) : AbstractSafe()

data class SafeWithInfo(val safe: Safe, val info: SafeInfo? = null) : AbstractSafe()
