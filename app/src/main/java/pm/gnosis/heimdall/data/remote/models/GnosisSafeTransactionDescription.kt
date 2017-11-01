package pm.gnosis.heimdall.data.remote.models

import pm.gnosis.models.Wei
import java.math.BigInteger

data class GnosisSafeTransactionDescription(
        val to: BigInteger,
        val value: Wei,
        val data: String,
        val operation: BigInteger,
        val nonce: BigInteger,
        val submittedAt: Long,
        val subject: String?,
        val transactionHash: String?
)