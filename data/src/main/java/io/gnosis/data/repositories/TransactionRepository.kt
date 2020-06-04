package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.TransactionDto
import io.gnosis.data.models.TransferDto
import io.gnosis.data.utils.formatBackendDate
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import java.math.BigInteger

class TransactionRepository(
    private val transactionServiceApi: TransactionServiceApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address): Page<Transaction> =
        transactionServiceApi.loadTransactions(safeAddress.asEthereumAddressChecksumString())
            .mapInner { transactionDto ->
                when {
                    transactionDto.transfers?.size == 1 -> transfer(transactionDto.transfers[0])
                    transactionDto.to == transactionDto.safe && transactionDto.data != null -> Transaction.SettingsChange(
                        transactionDto.nonce ?: BigInteger.valueOf(-1)
                    )
                    else -> Transaction.Custom(transactionDto.nonce ?: BigInteger.valueOf(-1))
                }
            }

    private fun transfer(transferDto: TransferDto): Transaction.Transfer =
        Transaction.Transfer(
            transferDto.to,
            transferDto.from,
            transferDto.value,
            transferDto.executionDate?.formatBackendDate(),
            transferDto.tokenAddress?.let { null } ?: TokenRepository.ETH_SERVICE_TOKEN_INFO
        )

}
