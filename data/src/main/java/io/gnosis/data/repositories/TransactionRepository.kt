package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.Transaction
import io.gnosis.data.utils.formatBackendDate
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity

class TransactionRepository(
    private val transactionServiceApi: TransactionServiceApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address): Page<Transaction> =
        transactionServiceApi.loadTransactions(safeAddress.asEthereumAddressChecksumString())
            .mapInner { transactionDto ->
                when {
                    transactionDto.to == safeAddress && transactionDto.data != null -> Transaction.SettingsChange(transactionDto.nonce)
                    transactionDto.data?.startsWith("0xa9059cbb") == true ->
                        Transaction.Transfer(
                            transactionDto.to,
                            transactionDto.value,
                            transactionDto.executionDate?.formatBackendDate(),
                            transactionDto.tokenAddress?.let { null } ?: TokenRepository.ETH_SERVICE_TOKEN_INFO
                        )
                    else -> Transaction.Custom(transactionDto.nonce)
                }
            }
}
