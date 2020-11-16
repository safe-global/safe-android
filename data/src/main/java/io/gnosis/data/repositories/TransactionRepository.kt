package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.*
import io.gnosis.data.models.transaction.*
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger

class TransactionRepository(
    private val gatewayApi: GatewayApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address): Page<Transaction> =
        gatewayApi.loadTransactions(safeAddress.asEthereumAddressChecksumString())

    suspend fun loadTransactionsPage(pageLink: String): Page<Transaction> =
        gatewayApi.loadTransactionsPage(pageLink)

    suspend fun getTransactionDetails(txId: String): TransactionDetails =
        gatewayApi.loadTransactionDetails(txId)

    suspend fun submitConfirmation(safeTxHash: String, signedSafeTxHash: String): TransactionDetails =
        gatewayApi.submitConfirmation(safeTxHash, TransactionConfirmationRequest(signedSafeTxHash))

    fun sign(ownerKey: BigInteger, safeTxHash: String): String =
        KeyPair.fromPrivate(ownerKey.toByteArray())
            .sign(safeTxHash.hexToByteArray())
            .toSignatureString()

    private fun ECDSASignature.toSignatureString() =
        r.toString(16).padStart(64, '0').substring(0, 64) +
                s.toString(16).padStart(64, '0').substring(0, 64) +
                v.toString(16).padStart(2, '0')

}

fun List<Param>?.getAddressValueByName(name: String): Solidity.Address? {
    return this?.find {
        it is Param.AddressParam && it.name == name
    }?.value as Solidity.Address?
}

fun List<Param>?.getIntValueByName(name: String): String? {
    return this?.find {
        it is Param.ValueParam && it.name == name
    }?.value as String?
}

fun String.dataSizeBytes(): Long = removeHexPrefix().hexToByteArray().size.toLong()
fun String?.hexStringNullOrEmpty(): Boolean = this?.dataSizeBytes() ?: 0L == 0L
