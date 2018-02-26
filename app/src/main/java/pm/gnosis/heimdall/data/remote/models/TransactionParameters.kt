package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import java.math.BigInteger

data class TransactionParameters(
    @Json(name = "gas")
    val gas: BigInteger,
    @Json(name = "gasPrice")
    val gasPrice: BigInteger,
    @Json(name = "nonce")
    val nonce: BigInteger
)
