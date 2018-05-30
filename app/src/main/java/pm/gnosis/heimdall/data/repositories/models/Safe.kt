package pm.gnosis.heimdall.data.repositories.models

import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import java.math.BigInteger

sealed class AbstractSafe

data class Safe(val address: Solidity.Address, val name: String? = null) : AbstractSafe()

data class PendingSafe(val hash: BigInteger, val name: String?, val address: Solidity.Address, val payment: Wei, val isFunded: Boolean = false) :
    AbstractSafe()
