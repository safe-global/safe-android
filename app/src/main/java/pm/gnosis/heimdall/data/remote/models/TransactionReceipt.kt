package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import java.math.BigInteger


data class TransactionReceipt(
    @Json(name = "status") val status: BigInteger?,
    @Json(name = "transactionHash") val transactionHash: String,
    @Json(name = "contractAddress") val contractAddress: String?,
    @Json(name = "logs") val logs: List<Event>
) {
    data class Event(
        @Json(name = "logIndex") val logIndex: BigInteger,
        @Json(name = "data") val data: String,
        @Json(name = "topics") val topics: List<String>
    )
}