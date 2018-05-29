package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json

sealed class PushMessage(
    @Json(name = "type") val type: String
) {
    data class SendTransaction(
        @Json(name = "hash") val hash: String,
        @Json(name = "safe") val safe: String,
        @Json(name = "to") val to: String,
        @Json(name = "value") val value: String,
        @Json(name = "data") val data: String,
        @Json(name = "operation") val operation: String,
        @Json(name = "txGas") val txGas: String,
        @Json(name = "dataGas") val dataGas: String,
        @Json(name = "gasPrice") val gasPrice: String,
        @Json(name = "gasToken") val gasToken: String,
        @Json(name = "nonce") val nonce: String,
        @Json(name = "r") val r: String,
        @Json(name = "s") val s: String,
        @Json(name = "v") val v: String
    ) : PushMessage("sendTransaction") {
        companion object {
            fun fromMap(params: Map<String, String>) =
                SendTransaction(
                    params.getOrThrow("hash"),
                    params.getOrThrow("safe"),
                    params.getOrThrow("to"),
                    params.getOrThrow("value"),
                    params.getOrThrow("data"),
                    params.getOrThrow("operation"),
                    params.getOrThrow("txGas"),
                    params.getOrThrow("dataGas"),
                    params.getOrThrow("gasPrice"),
                    params.getOrThrow("gasToken"),
                    params.getOrThrow("nonce"),
                    params.getOrThrow("r"),
                    params.getOrThrow("s"),
                    params.getOrThrow("v")
                )
        }
    }

    data class ConfirmTransaction(
        @Json(name = "hash") val hash: String,
        @Json(name = "r") val r: String,
        @Json(name = "s") val s: String,
        @Json(name = "v") val v: String
    ) : PushMessage("confirmTransaction") {
        companion object {
            fun fromMap(params: Map<String, String>) =
                ConfirmTransaction(
                    params.getOrThrow("hash"),
                    params.getOrThrow("r"),
                    params.getOrThrow("s"),
                    params.getOrThrow("v")
                )
        }
    }

    data class RejectTransaction(
        @Json(name = "hash") val hash: String,
        @Json(name = "r") val r: String,
        @Json(name = "s") val s: String,
        @Json(name = "v") val v: String
    ) : PushMessage("rejectTransaction") {
        companion object {
            fun fromMap(params: Map<String, String>) =
                    RejectTransaction(
                        params.getOrThrow("hash"),
                        params.getOrThrow("r"),
                        params.getOrThrow("s"),
                        params.getOrThrow("v")
                    )
        }
    }

    companion object {
        fun fromMap(params: Map<String, String>) =
            when (params["type"]) {
                "sendTransaction" -> SendTransaction.fromMap(params)
                "confirmTransaction" -> ConfirmTransaction.fromMap(params)
                "rejectTransaction" -> RejectTransaction.fromMap(params)
                else -> throw IllegalArgumentException("Unknown push type")
            }
    }
}

private fun Map<String, String>.getOrThrow(key: String) =
    get("hash") ?: throw IllegalArgumentException("Missing param $key")
