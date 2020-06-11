package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.*
import io.gnosis.data.utils.formatBackendDate
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger

class TransactionRepository(
    private val transactionServiceApi: TransactionServiceApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address): Page<Transaction> =
        transactionServiceApi.loadTransactions(safeAddress.asEthereumAddressChecksumString())
            .foldInner { transactionDto, accumulatedTransactions ->
                accumulatedTransactions.apply {
                    when (transactionDto) {
                        is ModuleTransactionDto -> add(custom(transactionDto))
                        is EthereumTransactionDto -> {
                            when {
                                !transactionDto.transfers.isNullOrEmpty() -> addAll(transactionDto.transfers.map { transfer(it) })
                                transactionDto.transfers.isNullOrEmpty() && transactionDto.data != null -> add(custom(transactionDto))
                                else -> add(transfer(transactionDto))
                            }
                        }
                        is MultisigTransactionDto -> {
                            when {
                                isSettingsChange(transactionDto) -> add(settings(transactionDto))
                                isErc20Transfer(transactionDto) -> add(transferErc20(transactionDto))
                                isErc721Transfer(transactionDto) -> add(transferErc721(transactionDto))
                                isEthTransfer(transactionDto) -> add(transferEth(transactionDto))
                                else -> add(custom(transactionDto))
                            }
                        }
                        else -> add(custom(transactionDto))
                    }
                }
            }

    private fun isErc721Transfer(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.operation == Operation.CALL &&
                transactionDto.contractInfo?.type == ContractInfoType.ERC721 &&
                listOf("safeTransferFrom", "transferFrom").contains(transactionDto.dataDecoded?.method)

    private fun isErc20Transfer(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.operation == Operation.CALL &&
                transactionDto.contractInfo?.type == ContractInfoType.ERC20 &&
                listOf("transfer", "transferFrom").contains(transactionDto.dataDecoded?.method)

    private fun isEthTransfer(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.data == null && transactionDto.operation == Operation.CALL

    private fun isSettingsChange(transactionDto: MultisigTransactionDto): Boolean =
        (transactionDto.to == transactionDto.safe
                && transactionDto.operation == Operation.CALL
                && SafeRepository.isSettingsMethod(transactionDto.dataDecoded?.method))

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
            transaction.value ?: BigInteger.ZERO,
            transaction.blockTimestamp?.formatBackendDate(),
            TokenRepository.ETH_SERVICE_TOKEN_INFO
        )

    //when contractInfo is available have when for ETH, ERC20 and ERC721
    private fun transferEth(transaction: MultisigTransactionDto): Transaction.Transfer =
        Transaction.Transfer(
            transaction.to,
            transaction.safe,
            transaction.value,
            transaction.executionDate?.formatBackendDate(),
            TokenRepository.ETH_SERVICE_TOKEN_INFO
        )

    private fun transferErc20(transaction: MultisigTransactionDto): Transaction.Transfer =
        Transaction.Transfer(
            transaction.to,
            transaction.safe,
            transaction.value,
            transaction.executionDate?.formatBackendDate(),
            TokenRepository.ETH_SERVICE_TOKEN_INFO // find out correct token data source
        )

    private fun transferErc721(transaction: MultisigTransactionDto): Transaction.Transfer =
        Transaction.Transfer(
            transaction.to,
            transaction.safe,
            transaction.value,
            transaction.executionDate?.formatBackendDate(),
            TokenRepository.ETH_SERVICE_TOKEN_INFO // find out correct token data source
        )

    private fun settings(transaction: MultisigTransactionDto): Transaction.SettingsChange =
        Transaction.SettingsChange(
            transaction.dataDecoded!!,
            transaction.executionDate?.formatBackendDate(),
            transaction.nonce
        )

    private fun custom(transaction: ModuleTransactionDto): Transaction.Custom =
        Transaction.Custom(
            transaction.nonce,
            transaction.module,
            transaction.data?.dataSizeBytes() ?: 0L,
            transaction.created,
            transaction.value ?: BigInteger.ZERO
        )

    private fun custom(transaction: MultisigTransactionDto): Transaction.Custom =
        Transaction.Custom(
            transaction.nonce,
            transaction.to,
            transaction.data?.dataSizeBytes() ?: 0L,
            transaction.creationDate,
            transaction.value
        )

    private fun custom(transaction: EthereumTransactionDto): Transaction.Custom =
        Transaction.Custom(
            null, // Ethereum txs do not have a nonce
            transaction.from,
            transaction.data?.dataSizeBytes() ?: 0L,
            transaction.blockTimestamp,
            transaction.value ?: BigInteger.ZERO
        )

    private fun custom(transaction: TransactionDto): Transaction.Custom =
        Transaction.Custom(
            null,
            transaction.to,
            0L,
            null,
            BigInteger.ZERO
        )

}

fun String.dataSizeBytes(): Long = removeHexPrefix().hexToByteArray().size.toLong()
