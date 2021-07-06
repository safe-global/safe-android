package io.gnosis.safe.notifications.models

import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

sealed class PushNotification(
    val type: String
) {

    abstract val safe: Solidity.Address

    data class ConfirmationRequest(
        override val safe: Solidity.Address,
        val safeTxHash: String,
        val chainId: Int
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "CONFIRMATION_REQUEST"
            fun fromMap(params: Map<String, String>) =
                ConfirmationRequest(
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("safeTxHash"),
                    params.getOrThrow("chainId").toInt()
                )
        }
    }

    data class ExecutedTransaction(
        override val safe: Solidity.Address,
        val safeTxHash: String,
        val failed: Boolean,
        val chainId: Int
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "EXECUTED_MULTISIG_TRANSACTION"
            fun fromMap(params: Map<String, String>) =
                ExecutedTransaction(
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("safeTxHash"),
                    params.getOrThrow("failed").toBoolean(),
                    params.getOrThrow("chainId").toInt()
                )
        }
    }

    data class IncomingToken(
        override val safe: Solidity.Address,
        val txHash: String,
        val tokenAddress: Solidity.Address,
        val value: BigInteger? = null, // null for ERC721 tokens
        val tokenId: String? = null,    // null for ERC20 tokens
        val chainId: Int
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "INCOMING_TOKEN"
            fun fromMap(params: Map<String, String>) =
                IncomingToken(
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("txHash"),
                    params.getOrThrow("tokenAddress").asEthereumAddress()!!,
                    params["value"]?.toBigInteger(),
                    params["tokenId"],
                    params.getOrThrow("chainId").toInt()
                )
        }
    }

    data class IncomingEther(
        override val safe: Solidity.Address,
        val txHash: String,
        val value: BigInteger,
        val chainId: Int
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "INCOMING_ETHER"
            fun fromMap(params: Map<String, String>) =
                IncomingEther(
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("txHash"),
                    params.getOrThrow("value").toBigInteger(),
                    params.getOrThrow("chainId").toInt()
                )
        }
    }

    companion object {
        fun fromMap(params: Map<String, String>) =
            when (params["type"]) {
                ConfirmationRequest.TYPE -> ConfirmationRequest.fromMap(params)
                ExecutedTransaction.TYPE -> ExecutedTransaction.fromMap(params)
                IncomingToken.TYPE -> IncomingToken.fromMap(params)
                IncomingEther.TYPE -> IncomingEther.fromMap(params)
                else -> throw IllegalArgumentException("Unknown push type")
            }
    }
}

private fun Map<String, String>.getOrThrow(key: String) =
    get(key) ?: throw IllegalArgumentException("Missing param $key")
