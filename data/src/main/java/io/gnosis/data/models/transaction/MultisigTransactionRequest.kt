package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class MultisigTransactionRequest(
    @Json(name = "to") val to: Solidity.Address,
    @Json(name = "value") val value: String = "0",
    @Json(name = "data") val data: String? = null,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "operation") val operation: Operation = Operation.CALL,
    @Json(name = "safeTxGas") val safeTxGas: String = "0",
    @Json(name = "baseGas") val baseGas: String = "0",
    @Json(name = "gasPrice") val gasPrice: String = "0",
    @Json(name = "gasToken") val gasToken: Solidity.Address = "0x00".asEthereumAddress()!!,
    @Json(name = "refundReceiver") val refundReceiver: Solidity.Address? = null,
    @Json(name = "safeTxHash") val safeTxHash: String,
    @Json(name = "sender") val sender: Solidity.Address,
    @Json(name = "signature") val signature: String,
    @Json(name = "origin") val origin: String? = null
)
