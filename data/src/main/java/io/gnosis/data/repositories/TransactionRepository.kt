package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.models.Page
import io.gnosis.data.models.Transaction
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix

class TransactionRepository(
    private val gatewayApi: GatewayApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address): Page<Transaction> =
        gatewayApi.loadTransactions(safeAddress.asEthereumAddressChecksumString())

    suspend fun loadTransactionsPage(pageLink: String): Page<Transaction> =
        gatewayApi.loadTransactionsPage(pageLink)
}

fun List<ParamsDto>?.getValueByName(name: String): String? {
    this?.map {
        if (it.name == name) {
            return it.value
        }
    }
    return null
}

fun String.dataSizeBytes(): Long = removeHexPrefix().hexToByteArray().size.toLong()
fun String?.hexStringNullOrEmpty(): Boolean = this?.dataSizeBytes() ?: 0L == 0L
