package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.dto.*
import io.gnosis.data.models.*
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger
import java.util.*

class TransactionRepository(
    private val gatewayApi: GatewayApi
) {

    suspend fun getTransactions(safeAddress: Solidity.Address): Page<Transaction> =
        gatewayApi.loadTransactions(safeAddress.asEthereumAddressChecksumString()).let { page ->
            val mappedResults = page.results.map { it.toTransaction() }
            Page(
                results = mappedResults,
                count = mappedResults.size,
                next = page.next,
                previous = page.previous
            )
        }

    suspend fun loadTransactionsPage(pageLink: String): Page<Transaction> =
        gatewayApi.loadTransactionsPage(pageLink).let { page ->
            val mappedResults = page.results.map { it.toTransaction() }
            Page(
                results = mappedResults,
                count = mappedResults.size,
                next = page.next,
                previous = page.previous
            )
        }

    suspend fun getTransactionDetails(txId: String): TransactionDetails =
        gatewayApi.loadTransactionDetails(txId).let { transactionDetailsDto ->
            return transactionDetailsDto.toTransactionDetails()
        }

    private fun GateTransactionDetailsDto.toTransactionDetails(): TransactionDetails =
        TransactionDetails(
            txHash = txHash,
            detailedExecutionInfo = detailedExecutionInfo?.toDetailedExecutionInfo(),
            executedAt = executedAt?.toDate(),
            txStatus = txStatus,
            txData = txData?.toTxData(),
            txInfo = txInfo.toTransactionInfo()
        )

    private fun DetailedExecutionInfoDto.toDetailedExecutionInfo(): DetailedExecutionInfo? =

        when (this) {
            is DetailedExecutionInfoDto.MultisigExecutionDetailsDto -> DetailedExecutionInfo.MultisigExecutionDetails(
                submittedAt = submittedAt.toDate(),
                nonce = nonce,
                safeTxHash = safeTxHash,
                signers = signers,
                confirmationsRequired = confirmationsRequired,
                confirmations = confirmations.toConfirmations(),
                executor = executor
            )
            is DetailedExecutionInfoDto.ModuleExecutionDetailsDto -> DetailedExecutionInfo.ModuleExecutionDetails(
                address = address
            )
        }

    private fun TransactionInfoDto.toTransactionInfo(): TransactionInfo =
        when (this) {
            is TransactionInfoDto.Custom ->
                TransactionInfo.Custom(
                    to = to,
                    dataSize = dataSize,
                    value = value
                )
            is TransactionInfoDto.SettingsChange ->
                TransactionInfo.SettingsChange(
                    dataDecoded = dataDecoded
                )
            is TransactionInfoDto.Transfer ->
                TransactionInfo.Transfer(
                    sender = sender,
                    recipient = recipient,
                    transferInfo = transferInfo.toTransferInfo(),
                    direction = direction
                )
            is TransactionInfoDto.Creation -> TransactionInfo.Creation(
                creator = creator,
                factory = factory,
                implementation = implementation,
                transactionHash = transactionHash
            )
            is TransactionInfoDto.Unknown -> TransactionInfo.Unknown
        }

    private fun TxDataDto.toTxData(): TxData? =
        TxData(
            hexData = hexData,
            dataDecoded = dataDecoded,
            to = to,
            value = value,
            operation = operation
        )

    private fun TransferInfoDto.toTransferInfo(): TransferInfo =
        when (this) {
            is TransferInfoDto.Erc20Transfer -> {
                TransferInfo.Erc20Transfer(
                    tokenAddress = tokenAddress,
                    value = value,
                    decimals = decimals,
                    logoUri = logoUri,
                    tokenName = tokenName,
                    tokenSymbol = tokenSymbol
                )
            }
            is TransferInfoDto.Erc721Transfer -> TransferInfo.Erc721Transfer(
                tokenAddress = tokenAddress,
                tokenSymbol = tokenSymbol,
                tokenName = tokenName,
                logoUri = logoUri,
                tokenId = tokenId
            )
            is TransferInfoDto.EtherTransfer -> TransferInfo.EtherTransfer(
                value = value
            )
        }

    private fun List<ConfirmationsDto>.toConfirmations(): List<Confirmations> =
        this.map { confirmation ->
            Confirmations(
                signer = confirmation.signer,
                signature = confirmation.signature,
                submittedAt = confirmation.submittedAt.toDate()
            )
        }

    private fun GateTransactionDto.toTransaction(): Transaction {
        return when (txInfo) {
            is TransactionInfoDto.Transfer -> Transaction.Transfer(
                id = id,
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce,
                date = timestamp.toDate(),
                recipient = txInfo.recipient,
                sender = txInfo.sender,
                value = txInfo.transferInfo.value(),
                tokenInfo = txInfo.transferInfo.tokenInfo(),
                incoming = txInfo.direction == TransactionDirection.INCOMING
            )
            is TransactionInfoDto.SettingsChange -> Transaction.SettingsChange(
                id = id,
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce ?: BigInteger.ZERO,
                date = timestamp.toDate(),
                dataDecoded = txInfo.dataDecoded
            )
            is TransactionInfoDto.Custom -> Transaction.Custom(
                id = id,
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce,
                date = timestamp.toDate(),
                address = txInfo.to,
                dataSize = txInfo.dataSize,
                value = txInfo.value
            )
            is TransactionInfoDto.Creation -> Transaction.Creation(
                id = id,
                confirmations = null,
                status = TransactionStatus.SUCCESS,
                executionInfo = null,
                timestamp = timestamp.toDate(),
                txInfo = TransactionInfo.Creation(
                    creator = txInfo.creator,
                    transactionHash = txInfo.transactionHash,
                    implementation = txInfo.implementation,
                    factory = txInfo.factory
                )
            )
            is TransactionInfoDto.Unknown -> Transaction.Custom(
                id = id,
                address = Solidity.Address(BigInteger.ZERO),
                status = TransactionStatus.SUCCESS,
                value = BigInteger.ZERO,
                dataSize = 0,
                confirmations = null,
                nonce = BigInteger.ZERO,
                date = null
            )
        }
    }

    private fun TransferInfoDto.value(): BigInteger =
        when (this) {
            is TransferInfoDto.Erc20Transfer -> value
            is TransferInfoDto.Erc721Transfer -> BigInteger.ONE
            is TransferInfoDto.EtherTransfer -> value
        }

    private fun TransferInfoDto.tokenInfo(): ServiceTokenInfo? =
        when (this) {
            is TransferInfoDto.Erc20Transfer -> ServiceTokenInfo(
                address = tokenAddress,
                decimals = decimals ?: 0,
                symbol = tokenSymbol.orEmpty(),
                name = tokenName.orEmpty(),
                logoUri = logoUri,
                type = ServiceTokenInfo.TokenType.ERC20
            )
            is TransferInfoDto.Erc721Transfer -> ServiceTokenInfo(
                address = tokenAddress,
                symbol = tokenSymbol.orEmpty(),
                name = tokenName.orEmpty(),
                logoUri = logoUri,
                type = ServiceTokenInfo.TokenType.ERC721
            )
            is TransferInfoDto.EtherTransfer -> ETH_SERVICE_TOKEN_INFO
        }

    private fun Long.toDate(): Date = Date(this)
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

