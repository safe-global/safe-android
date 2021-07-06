package io.gnosis.safe.notifications.models

import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

sealed class PushNotification(
    val type: String
) {
    abstract val chainId: Int
    abstract val safe: Solidity.Address

    data class ConfirmationRequest(
        override val chainId: Int,
        override val safe: Solidity.Address,
        val safeTxHash: String
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "CONFIRMATION_REQUEST"
            fun fromMap(params: Map<String, String>) =
                ConfirmationRequest(
                    params.getOrThrow("chainId").toInt(),
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("safeTxHash")
                )
        }
    }

    data class ExecutedTransaction(
        override val chainId: Int,
        override val safe: Solidity.Address,
        val safeTxHash: String,
        val failed: Boolean
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "EXECUTED_MULTISIG_TRANSACTION"
            fun fromMap(params: Map<String, String>) =
                ExecutedTransaction(
                    params.getOrThrow("chainId").toInt(),
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("safeTxHash"),
                    params.getOrThrow("failed").toBoolean()
                )
        }
    }

    data class IncomingToken(
        override val chainId: Int,
        override val safe: Solidity.Address,
        val txHash: String,
        val tokenAddress: Solidity.Address, // null for ERC721 tokens
        val value: BigInteger? = null,    // null for ERC20 tokens
        val tokenId: String? = null
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "INCOMING_TOKEN"
            fun fromMap(params: Map<String, String>) =
                IncomingToken(
                    params.getOrThrow("chainId").toInt(),
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("txHash"),
                    params.getOrThrow("tokenAddress").asEthereumAddress()!!,
                    params["value"]?.toBigInteger(),
                    params["tokenId"]
                )
        }
    }

    data class IncomingEther(
        override val chainId: Int,
        override val safe: Solidity.Address,
        val txHash: String,
        val value: BigInteger
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "INCOMING_ETHER"
            fun fromMap(params: Map<String, String>) =
                IncomingEther(
                    params.getOrThrow("chainId").toInt(),
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("txHash"),
                    params.getOrThrow("value").toBigInteger()
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
