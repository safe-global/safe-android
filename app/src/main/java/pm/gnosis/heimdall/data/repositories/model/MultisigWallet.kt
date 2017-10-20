package pm.gnosis.heimdall.data.repositories.model

import java.math.BigInteger

data class MultisigWallet(val address: BigInteger, val name: String? = null)
