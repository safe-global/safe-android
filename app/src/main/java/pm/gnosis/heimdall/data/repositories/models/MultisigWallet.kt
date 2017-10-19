package pm.gnosis.heimdall.data.repositories.models

import java.math.BigInteger

data class MultisigWallet(val address: BigInteger, val name: String? = null)
