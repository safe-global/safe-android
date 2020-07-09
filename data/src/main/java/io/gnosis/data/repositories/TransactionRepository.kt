package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.*
import io.gnosis.data.models.*
import io.gnosis.data.repositories.TokenRepository.Companion.ERC20_FALLBACK_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.TokenRepository.Companion.ERC721_FALLBACK_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.gnosis.data.utils.formatBackendDate
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.decimalAsBigInteger
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger

class TransactionRepository(
    private val transactionServiceApi: TransactionServiceApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address, safeInfo: SafeInfo): Page<Transaction> =
        transactionServiceApi.loadTransactions(safeAddress.asEthereumAddressChecksumString())
            .foldInner { transactionDto, accumulatedTransactions ->
                accumulatedTransactions.apply {
                    when (transactionDto) {
                        is ModuleTransactionDto -> add(custom(transactionDto))
                        is EthereumTransactionDto -> {
                            when {
                                !transactionDto.transfers.isNullOrEmpty() -> addAll(transactionDto.transfers.map { transfer(it) })
                                transactionDto.transfers.isNullOrEmpty() && !transactionDto.data.hexStringNullOrEmpty() -> add(custom(transactionDto))
                                else -> add(transfer(transactionDto))
                            }
                        }
                        is MultisigTransactionDto -> {
                            when {
                                isSettingsChange(transactionDto) -> add(settings(transactionDto, safeInfo))
                                isErc20Transfer(transactionDto) -> add(transferErc20(transactionDto, safeInfo))
                                isErc721Transfer(transactionDto) -> add(transferErc721(transactionDto, safeInfo))
                                isEthTransfer(transactionDto) -> add(transferEth(transactionDto, safeInfo))
                                else -> add(custom(transactionDto, safeInfo))
                            }
                        }
                        else -> add(custom(transactionDto))
                    }
                }
            }

    private fun isErc721Transfer(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.operation == Operation.CALL &&
                //transactionDto.contractInfo?.type == ContractInfoType.ERC721 && // TODO enable this check when we have contractInfo
                listOf("safeTransferFrom", "transferFrom").contains(transactionDto.dataDecoded?.method) &&
                transactionDto.dataDecoded?.parameters?.getValueByName("tokenId") != null // TODO Remove this when have contractInfo

    private fun isErc20Transfer(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.operation == Operation.CALL &&
//                transactionDto.contractInfo?.type == ContractInfoType.ERC20 && // TODO enable this check when we have contractInfo
                listOf("transfer", "transferFrom").contains(transactionDto.dataDecoded?.method) &&
                transactionDto.dataDecoded?.parameters?.getValueByName("value") != null // TODO Remove this when have contractInfo

    private fun isEthTransfer(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.data.hexStringNullOrEmpty() && transactionDto.operation == Operation.CALL

    private fun isSettingsChange(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.to == transactionDto.safe &&
                transactionDto.operation == Operation.CALL &&
                SafeRepository.isSettingsMethod(transactionDto.dataDecoded?.method)

    // This is a big assumption for txType == ETHEREUM_TRANSACTION, it was agreed that this can be assumed successful, because only successful TXs trigger events
    private fun transfer(transferDto: TransferDto): Transaction.Transfer {
        val tokenInfo = serviceTokenInfo(transferDto)
        val value = when (tokenInfo.type) {
            ServiceTokenInfo.TokenType.ERC721 -> BigInteger.ONE
            else -> transferDto.value ?: BigInteger.ZERO
        }
        return Transaction.Transfer(
            TransactionStatus.Success,
            null,
            transferDto.to,
            transferDto.from,
            value,
            transferDto.executionDate?.formatBackendDate(),
            tokenInfo,
            null
        )
    }

    // This is a big assumption for txType == ETHEREUM_TRANSACTION, it was agreed that this can be assumed successful, because only successful TXs trigger events
    private fun transfer(transaction: EthereumTransactionDto): Transaction.Transfer =
        Transaction.Transfer(
            TransactionStatus.Success,
            null,
            transaction.to,
            transaction.from,
            transaction.value ?: BigInteger.ZERO,
            transaction.blockTimestamp?.formatBackendDate(),
            ETH_SERVICE_TOKEN_INFO,
            null
        )

    // when contractInfo is available have when for ETH, ERC20 and ERC721
    private fun transferEth(transaction: MultisigTransactionDto, safeInfo: SafeInfo): Transaction.Transfer =
        Transaction.Transfer(
            transaction.status(safeInfo),
            transaction.confirmations?.size ?: 0,
            transaction.to,
            transaction.safe,
            transaction.value,
            transaction.bestAvailableDate(),
            ETH_SERVICE_TOKEN_INFO,
            transaction.nonce
        )

    private fun transferErc20(transaction: MultisigTransactionDto, safeInfo: SafeInfo): Transaction.Transfer {
        val to = transaction.dataDecoded?.parameters?.getValueByName("to")?.asEthereumAddress() ?: Solidity.Address(BigInteger.ZERO)
        val value = transaction.dataDecoded?.parameters?.getValueByName("value")?.decimalAsBigInteger() ?: BigInteger.ZERO

        // Only available with transferFrom
        val from = transaction.dataDecoded?.parameters?.getValueByName("from")?.asEthereumAddress()

        val tokenInfo: ServiceTokenInfo = serviceTokenInfoErc20(transaction)

        return Transaction.Transfer(
            transaction.status(safeInfo),
            transaction.confirmations?.size ?: 0,
            to,
            from ?: transaction.safe,
            value,
            transaction.bestAvailableDate(),
            tokenInfo,
            transaction.nonce
        )
    }

    private fun transferErc721(transaction: MultisigTransactionDto, safeInfo: SafeInfo): Transaction.Transfer {
        val from = transaction.dataDecoded?.parameters?.getValueByName("from")?.asEthereumAddress() ?: Solidity.Address(BigInteger.ZERO)
        val to = transaction.dataDecoded?.parameters?.getValueByName("to")?.asEthereumAddress() ?: Solidity.Address(BigInteger.ZERO)
        val value = BigInteger.ONE

        val tokenInfo = serviceTokenInfoErc721(transaction)

        return Transaction.Transfer(
            transaction.status(safeInfo),
            transaction.confirmations?.size ?: 0,
            to,
            from,
            value,
            transaction.bestAvailableDate(),
            tokenInfo,
            transaction.nonce
        )
    }

    private fun serviceTokenInfo(transfer: TransferDto): ServiceTokenInfo =
        transfer.tokenInfo
            ?: transfer.tokenAddress?.let {
                ERC721_FALLBACK_SERVICE_TOKEN_INFO
            } ?: ETH_SERVICE_TOKEN_INFO

    private fun serviceTokenInfoErc20(transaction: MultisigTransactionDto): ServiceTokenInfo {
        return if (transaction.transfers != null && transaction.transfers.isNotEmpty()) {
            serviceTokenInfo(transaction.transfers[0])
        } else {
            ERC20_FALLBACK_SERVICE_TOKEN_INFO
        }
    }

    private fun serviceTokenInfoErc721(transaction: MultisigTransactionDto): ServiceTokenInfo {
        return if (transaction.transfers != null && transaction.transfers.isNotEmpty()) {
            serviceTokenInfo(transaction.transfers[0])
        } else {
            ERC721_FALLBACK_SERVICE_TOKEN_INFO
        }
    }


    private fun settings(transaction: MultisigTransactionDto, safeInfo: SafeInfo): Transaction.SettingsChange =
        Transaction.SettingsChange(
            transaction.status(safeInfo),
            transaction.confirmations?.size ?: 0,
            transaction.dataDecoded!!,
            transaction.bestAvailableDate(),
            transaction.nonce
        )

    private fun custom(transaction: ModuleTransactionDto): Transaction.Custom =
        Transaction.Custom(
            status = TransactionStatus.Success,
            confirmations = null,
            nonce = transaction.nonce,
            address = transaction.module,
            dataSize = transaction.data?.dataSizeBytes() ?: 0L,
            date = transaction.created?.formatBackendDate(),
            value = transaction.value ?: BigInteger.ZERO
        )

    private fun custom(transaction: MultisigTransactionDto, safeInfo: SafeInfo): Transaction.Custom =
        Transaction.Custom(
            transaction.status(safeInfo),
            confirmations = transaction.confirmations?.size ?: 0,
            nonce = transaction.nonce,
            address = transaction.to,
            dataSize = transaction.data?.dataSizeBytes() ?: 0L,
            date = transaction.bestAvailableDate(),
            value = transaction.value
        )

    // This is a big assumption for txType == ETHEREUM_TRANSACTION, it was agreed that this can be assumed successful, because only successful TXs trigger events
    private fun custom(transaction: EthereumTransactionDto): Transaction.Custom =
        Transaction.Custom(
            status = TransactionStatus.Success,
            confirmations = null,
            nonce = null, // Ethereum txs do not have a nonce
            address = transaction.to,
            dataSize = transaction.data?.dataSizeBytes() ?: 0L,
            date = transaction.blockTimestamp,
            value = transaction.value ?: BigInteger.ZERO
        )

    private fun custom(transaction: TransactionDto): Transaction.Custom {
        val status = TransactionStatus.Success
        val confirmations = 0
        val date = null

        return Transaction.Custom(
            status = status,
            confirmations = confirmations,
            nonce = null,
            address = transaction.to,
            dataSize = transaction.data?.dataSizeBytes() ?: 0L,
            date = date?.formatBackendDate(),
            value = BigInteger.ZERO
        )
    }

    // TODO Pending not reachable yet
    private fun MultisigTransactionDto.status(safeInfo: SafeInfo): TransactionStatus =
        when {
            isExecuted && isSuccessful == true -> TransactionStatus.Success
            isExecuted && isSuccessful != true -> TransactionStatus.Failed
            !isExecuted && nonce < safeInfo.nonce -> TransactionStatus.Cancelled
            !isExecuted && nonce >= safeInfo.nonce && confirmations?.size?.compareTo(safeInfo.threshold) ?: -1 < 0 -> TransactionStatus.AwaitingConfirmations
            else -> TransactionStatus.AwaitingExecution
        }
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
fun MultisigTransactionDto.bestAvailableDate() = (executionDate ?: submissionDate ?: modified)?.formatBackendDate()
