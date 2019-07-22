package pm.gnosis.heimdall.data.remote.models.push

import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexToByteArray
import java.math.BigInteger

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

    data class SignTypedDataConfirmation(
        val hash: ByteArray,
        val signature: ByteArray
    ) : PushMessage(TYPE) {
        companion object {
            const val TYPE = "signTypedDataConfirmation"
            fun fromMap(params: Map<String, String>) =
                SignTypedDataConfirmation(
                    hash = params.getOrThrow("hash").hexToByteArray(),
                    signature = params.getOrThrow("signature").hexToByteArray()
                )
        }
    }

    data class RejectSignTypedData(
        val hash: String,
        val r: String,
        val s: String,
        val v: String
    ) : PushMessage(TYPE) {
        companion object {
            const val TYPE = "rejectSignTypedData"
            fun fromMap(params: Map<String, String>) =
                RejectSignTypedData(
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
                SendTransaction.TYPE -> SendTransaction.fromMap(params)
                ConfirmTransaction.TYPE -> ConfirmTransaction.fromMap(params)
                RejectTransaction.TYPE -> RejectTransaction.fromMap(params)
                SafeCreation.TYPE -> SafeCreation.fromMap(params)
                SignTypedData.TYPE -> SignTypedData.fromMap(params)
                SignTypedDataConfirmation.TYPE -> SignTypedDataConfirmation.fromMap(params)
                RejectSignTypedData.TYPE -> RejectSignTypedData.fromMap(params)
                else -> throw IllegalArgumentException("Unknown push type")
            }
    }
}

private fun Map<String, String>.getOrThrow(key: String) =
    get(key) ?: throw IllegalArgumentException("Missing param $key")
