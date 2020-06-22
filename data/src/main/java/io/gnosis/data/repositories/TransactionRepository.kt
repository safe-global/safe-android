package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.*
import io.gnosis.data.models.*
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.gnosis.data.utils.formatBackendDate
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.decimalAsBigInteger
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger

// TODO Remove these fake tokens, when we have ContractInfo from the backend
private val defaultErc20Address = "0xc778417e063141139fce010982780140aa0cd5ab".asEthereumAddress()!!
private val defaultErc721Address = "0xB3775fB83F7D12A36E0475aBdD1FCA35c091efBe".asEthereumAddress()!!
val FAKE_ERC20_TOKEN_INFO = ServiceTokenInfo(defaultErc20Address, 18, "WETH", "Wrapped Ether", "local::ethereum")
val FAKE_ERC721_TOKEN_INFO = ServiceTokenInfo(defaultErc721Address, 18, "DRK", "Dirk", "local::ethereum")

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
                                transactionDto.transfers.isNullOrEmpty() && !transactionDto.data.hexStringNullOrEmpty() -> add(custom(transactionDto))
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
                transactionDto.contractInfo?.type == ContractInfoType.ERC721 && // Always false unless we have contractInfo
                listOf("safeTransferFrom", "transferFrom").contains(transactionDto.dataDecoded?.method)

    private fun isErc20Transfer(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.operation == Operation.CALL &&
//                transactionDto.contractInfo?.type == ContractInfoType.ERC20 && //TODO enable this check when we have contractInfo
                listOf("transfer", "transferFrom").contains(transactionDto.dataDecoded?.method)

    private fun isEthTransfer(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.data.hexStringNullOrEmpty() && transactionDto.operation == Operation.CALL

    private fun isSettingsChange(transactionDto: MultisigTransactionDto): Boolean =
        transactionDto.to == transactionDto.safe &&
                transactionDto.operation == Operation.CALL &&
                SafeRepository.isSettingsMethod(transactionDto.dataDecoded?.method)

    private fun transfer(transferDto: TransferDto): Transaction.Transfer {
        return Transaction.Transfer(
            transferDto.to,
            transferDto.from,
            transferDto.value ?: BigInteger.ZERO,
            transferDto.executionDate?.formatBackendDate(),
            transferDto.type.let {
                when (it) {
                    TransferType.ERC20_TRANSFER -> FAKE_ERC20_TOKEN_INFO
                    TransferType.ERC721_TRANSFER -> FAKE_ERC721_TOKEN_INFO
                    else -> ETH_SERVICE_TOKEN_INFO
                }
            }
        )
    }

    private fun transfer(transaction: EthereumTransactionDto): Transaction.Transfer =
        Transaction.Transfer(
            transaction.to,
            transaction.from,
            transaction.value ?: BigInteger.ZERO,
            transaction.blockTimestamp?.formatBackendDate(),
            ETH_SERVICE_TOKEN_INFO
        )

    // when contractInfo is available have when for ETH, ERC20 and ERC721
    private fun transferEth(transaction: MultisigTransactionDto): Transaction.Transfer =
        Transaction.Transfer(
            transaction.to,
            transaction.safe,
            transaction.value,
            transaction.executionDate?.formatBackendDate(),
            ETH_SERVICE_TOKEN_INFO
        )

    private fun transferErc20(transaction: MultisigTransactionDto): Transaction.Transfer {
        val to = transaction.dataDecoded?.parameters?.getValueByName("to")?.asEthereumAddress() ?: Solidity.Address(BigInteger.ZERO)
        val value = transaction.dataDecoded?.parameters?.getValueByName("value")?.decimalAsBigInteger() ?: BigInteger.ZERO

        // Only available with transferFrom
        val from = transaction.dataDecoded?.parameters?.getValueByName("from")?.asEthereumAddress()

        return Transaction.Transfer(
            to,
            from ?: transaction.safe,
            value,
            transaction.executionDate?.formatBackendDate()
                ?: transaction.submissionDate?.formatBackendDate()
                ?: transaction.modified?.formatBackendDate(),
            FAKE_ERC20_TOKEN_INFO // TODO: find out correct token data source
        )
    }

    private fun transferErc721(transaction: MultisigTransactionDto): Transaction.Transfer {
        val from = transaction.dataDecoded?.parameters?.getValueByName("from")?.asEthereumAddress() ?: Solidity.Address(BigInteger.ZERO)
        val to = transaction.dataDecoded?.parameters?.getValueByName("to")?.asEthereumAddress() ?: Solidity.Address(BigInteger.ZERO)
        val value = BigInteger.ONE

        return Transaction.Transfer(
            to,
            from,
            value,
            transaction.executionDate?.formatBackendDate(),
            FAKE_ERC721_TOKEN_INFO // TODO: find out correct token data source
        )
    }

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
            transaction.data?.dataSizeBytes() ?: 0L,
            null,
            BigInteger.ZERO
        )
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
