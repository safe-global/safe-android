package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.*
import io.gnosis.data.utils.formatBackendDate
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import java.math.BigInteger

class TransactionRepository(
    private val transactionServiceApi: TransactionServiceApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address): Page<Transaction> =
        transactionServiceApi.loadTransactions(safeAddress.asEthereumAddressChecksumString())
            .fold { transactionDto, accumulatedTransactions ->
                accumulatedTransactions.apply {
                    when (transactionDto) {
                        is ModuleTransactionDto -> add(custom(transactionDto))
                        is EthereumTransactionDto -> {
                            when {
                                !transactionDto.transfers.isNullOrEmpty() -> addAll(transactionDto.transfers.map { transfer(it) })
                                transactionDto.transfers.isNullOrEmpty() && transactionDto.data != null -> add(custom(transactionDto))
                                else -> add(transfer(transactionDto))
                            }
                            add(transfer(transactionDto))
                        }
                        is MultisigTransactionDto -> {
                            when {
                                transactionDto.data == null
                                        && transactionDto.operation == Operation.CALL -> add(transfer(transactionDto))
                                transactionDto.to == transactionDto.safe
                                        && transactionDto.operation == Operation.CALL
                                        && SafeRepository.isSettingsMethod(transactionDto.dataDecoded?.method) -> add(settings(transactionDto))
                                else -> add(custom(transactionDto))
                            }
                        }
                        else -> add(custom(transactionDto))
                    }
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

    private fun transfer(transaction: EthereumTransactionDto): Transaction.Transfer =
        Transaction.Transfer(
            transaction.to,
            transaction.from,
            transaction.value ?: BigInteger.ONE,
            transaction.blockTimestamp?.formatBackendDate(),
            TokenRepository.ETH_SERVICE_TOKEN_INFO
        )

    //when contractInfo is available have when for ETH, ERC20 and ERC721
    private fun transfer(transaction: MultisigTransactionDto): Transaction.Transfer =
        Transaction.Transfer(
            transaction.to,
            transaction.safe,
            transaction.value,
            transaction.executionDate?.formatBackendDate(),
            TokenRepository.ETH_SERVICE_TOKEN_INFO
        )

    private fun settings(transaction: MultisigTransactionDto): Transaction.SettingsChange =
        Transaction.SettingsChange(transaction.dataDecoded!!, transaction.executionDate, transaction.nonce)

    private fun custom(transaction: ModuleTransactionDto): Transaction.Custom =
        Transaction.Custom(BigInteger.ZERO)

    private fun custom(transaction: MultisigTransactionDto): Transaction.Custom =
        Transaction.Custom(BigInteger.ZERO)

    private fun custom(transaction: EthereumTransactionDto): Transaction.Custom =
        Transaction.Custom(BigInteger.ZERO)

    private fun custom(transaction: TransactionDto): Transaction.Custom =
        Transaction.Custom(BigInteger.ZERO)
}
