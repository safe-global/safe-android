package pm.gnosis.heimdall.data.remote.models.push

import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.asEthereumAddress

sealed class PushMessage(
    val type: String
) {
    data class SendTransaction(
        val hash: String,
        val safe: String,
        val to: String,
        val value: String,
        val data: String,
        val operation: String,
        val txGas: String,
        val dataGas: String,
        val operationalGas: String,
        val gasPrice: String,
        val gasToken: String,
        val nonce: String,
        val r: String,
        val s: String,
        val v: String
    ) : PushMessage(TYPE) {
        companion object {
            const val TYPE = "sendTransaction"
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
                    params.getOrThrow("operationalGas"),
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
        val hash: String,
        val r: String,
        val s: String,
        val v: String
    ) : PushMessage(TYPE) {
        companion object {
            const val TYPE = "confirmTransaction"
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
        val hash: String,
        val r: String,
        val s: String,
        val v: String
    ) : PushMessage(TYPE) {
        companion object {
            const val TYPE = "rejectTransaction"
            fun fromMap(params: Map<String, String>) =
                RejectTransaction(
                    params.getOrThrow("hash"),
                    params.getOrThrow("r"),
                    params.getOrThrow("s"),
                    params.getOrThrow("v")
                )
        }
    }

    data class SafeCreation(val safe: String) : PushMessage(TYPE) {
        companion object {
            const val TYPE = "safeCreation"
            fun fromMap(params: Map<String, String>) =
                SafeCreation(params.getOrThrow("safe"))
        }
    }

    data class SignTypedData(
        val payload: String,
        val safe: Solidity.Address,
        val signature: Signature
    ) : PushMessage(TYPE) {
        companion object {
            const val TYPE = "signTypedData"
            fun fromMap(params: Map<String, String>) =
                SignTypedData(
                    payload = params.getOrThrow("payload"),
                    safe = params.getOrThrow("safe").asEthereumAddress()!!,
                    signature = Signature(
                        r = params.getOrThrow("r").toBigInteger(),
                        s = params.getOrThrow("s").toBigInteger(),
                        v = params.getOrThrow("v").toByte()
                    )
                )
        }
    }

    companion object {
        fun fromMap(params: Map<String, String>) =
            when (params["type"]) {
                "sendTransaction" -> SendTransaction.fromMap(params)
                "confirmTransaction" -> ConfirmTransaction.fromMap(params)
                "rejectTransaction" -> RejectTransaction.fromMap(params)
                "safeCreation" -> SafeCreation.fromMap(params)
                "signTypedData" -> SignTypedData.fromMap(params)
                else -> throw IllegalArgumentException("Unknown push type")
            }
    }
}

private fun Map<String, String>.getOrThrow(key: String) =
    get(key) ?: throw IllegalArgumentException("Missing param $key")
