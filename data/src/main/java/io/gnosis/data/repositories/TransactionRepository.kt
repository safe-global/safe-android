package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Page
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.data.utils.toSignatureString
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger

class TransactionRepository(
    private val gatewayApi: GatewayApi
) {

    suspend fun getQueuedTransactions(safe: Safe): Page<TxListEntry> =
        gatewayApi.loadTransactionsQueue(chainId = safe.chainId, address = safe.address.asEthereumAddressChecksumString())

    suspend fun getHistoryTransactions(safe: Safe): Page<TxListEntry> =
        gatewayApi.loadTransactionsHistory(chainId = safe.chainId, address = safe.address.asEthereumAddressChecksumString())

    suspend fun loadTransactionsPage(pageLink: String): Page<TxListEntry> =
        gatewayApi.loadTransactionsPage(pageLink)

    suspend fun getTransactionDetails(chainId: BigInteger, txId: String): TransactionDetails =
        gatewayApi.loadTransactionDetails(transactionId = txId, chainId = chainId)

    suspend fun submitConfirmation(safeTxHash: String, signedSafeTxHash: String, chainId: BigInteger): TransactionDetails =
        gatewayApi.submitConfirmation(safeTxHash = safeTxHash, txConfirmationRequest = TransactionConfirmationRequest(signedSafeTxHash), chainId = chainId)

    suspend fun estimateTransaction(
        chainId: BigInteger,
        safeAddress: Solidity.Address,
        to: Solidity.Address,
        value: BigInteger = BigInteger.ZERO,
        data: String = "",
        operation: Operation = Operation.CALL,
    ): TransactionEstimation {
        return gatewayApi.estimateTransaction(
            chainId,
            safeAddress.asEthereumAddressChecksumString(),
            TransactionEstimationRequest(
                to,
                value,
                data,
                operation,
                chainId
            )
        )
    }

    suspend fun proposeTransaction(
        chainId: BigInteger,
        safeAddress: Solidity.Address,
        value: BigInteger = BigInteger.ZERO,
        data: String? = null,
        nonce: BigInteger,
        operation: Operation = Operation.CALL,
        safeTxGas: Long = 0,
        baseGas: Long = 0,
        gasPrice: Long = 0,
        gasToken: Solidity.Address = "0x00".asEthereumAddress()!!,
        refundReceiver: Solidity.Address? = null,
        safeTxHash: String,
        sender: Solidity.Address,
        signature: String,
        origin: Solidity.Address? = null
    ) {
        gatewayApi.proposeTransaction(
            chainId  = chainId,
            safeAddress = safeAddress.asEthereumAddressChecksumString(),
            multisigTransactionRequest = MultisigTransactionRequest(
                to = safeAddress,
                value = value.toString(),
                data = data,
                nonce = nonce.toString(),
                operation = operation,
                safeTxGas = safeTxGas.toString(),
                baseGas = baseGas.toString(),
                gasPrice = gasPrice.toString(),
                gasToken = gasToken,
                refundReceiver = refundReceiver,
                safeTxHash = safeTxHash,
                sender = sender,
                signature = signature,
                origin = origin?.asEthereumAddressChecksumString()
            )
        )
    }

    fun sign(ownerKey: BigInteger, safeTxHash: String): String =
        KeyPair.fromPrivate(ownerKey.toByteArray())
            .sign(safeTxHash.hexToByteArray())
            .toSignatureString()
}

fun List<Param>?.getIntValueByName(name: String): String? {
    return this?.find {
        it is Param.Value && it.name == name
    }?.value as String?
}

fun String.dataSizeBytes(): Long = removeHexPrefix().hexToByteArray().size.toLong()
fun String?.hexStringNullOrEmpty(): Boolean = this?.dataSizeBytes() ?: 0L == 0L
