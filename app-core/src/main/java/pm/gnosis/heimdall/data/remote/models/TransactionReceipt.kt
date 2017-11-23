package pm.gnosis.heimdall.data.remote.models

import java.math.BigInteger


data class TransactionReceipt(
        val transactionHash: String,
        val contractAddress: String?,
        val logs: List<Event>
) {
    data class Event(val logIndex: BigInteger, val data: String, val topics: List<String>)
}