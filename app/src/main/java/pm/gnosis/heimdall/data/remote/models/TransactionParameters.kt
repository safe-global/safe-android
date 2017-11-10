package pm.gnosis.heimdall.data.remote.models

import java.math.BigInteger

data class TransactionParameters(val gas: BigInteger, val gasPrice: BigInteger, val nonce: BigInteger)