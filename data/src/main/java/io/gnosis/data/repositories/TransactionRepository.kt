package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.TransactionDto
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity

class TransactionRepository(
    private val transactionServiceApi: TransactionServiceApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address): List<TransactionDto> {
        return transactionServiceApi.loadTransactions(safeAddress.asEthereumAddressChecksumString())
            .let { it.results }
    }
}
