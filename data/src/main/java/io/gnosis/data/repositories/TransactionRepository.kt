package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.dto.Confirmations
import io.gnosis.data.backend.dto.DetailedExecutionInfo
import io.gnosis.data.backend.dto.Erc20Transfer
import io.gnosis.data.backend.dto.Erc721Transfer
import io.gnosis.data.backend.dto.EtherTransfer
import io.gnosis.data.backend.dto.GateTransactionDetailsDto
import io.gnosis.data.backend.dto.GateTransactionDto
import io.gnosis.data.backend.dto.ModuleExecutionDetails
import io.gnosis.data.backend.dto.MultisigExecutionDetails
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.backend.dto.TransactionInfo
import io.gnosis.data.backend.dto.TransferInfo
import io.gnosis.data.backend.dto.TxData
import io.gnosis.data.models.DomainConfirmations
import io.gnosis.data.models.DomainDetailedExecutionInfo
import io.gnosis.data.models.DomainTransactionDetails
import io.gnosis.data.models.DomainTransactionInfo
import io.gnosis.data.models.DomainTransferInfo
import io.gnosis.data.models.DomainTxData
import io.gnosis.data.models.Page
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.TransactionStatus
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

    suspend fun getTransactionDetails(txId: String): DomainTransactionDetails =
        gatewayApi.loadTransactionDetails(txId).let { transactionDto ->
            return transactionDto.toDomainTransactionDetails()
        }

    private fun GateTransactionDetailsDto.toDomainTransactionDetails(): DomainTransactionDetails =
        DomainTransactionDetails(
            txHash = txHash,
            detailedExecutionInfo = detailedExecutionInfo.toDomainDetailedExecutionInfo(),
            executedAt = executedAt,
            txStatus = txStatus,
            txData = txData?.toDomainTxData(),
            txInfo = txInfo.toDomainTransactionInfo()
        )

    private fun DetailedExecutionInfo?.toDomainDetailedExecutionInfo(): DomainDetailedExecutionInfo? =

        when (this) {
            is MultisigExecutionDetails -> DomainDetailedExecutionInfo.DomainMultisigExecutionDetails(
                submittedAt = submittedAt,
                nonce = nonce,
                safeTxHash = safeTxHash,
                signers = signers,
                confirmationsRequired = confirmationsRequired,
                confirmations = confirmations.toDomainConfirmations()
            )
            is ModuleExecutionDetails -> DomainDetailedExecutionInfo.DomainModuleExecutionDetails(
                address = address
            )
            else -> null // TODO  get rid of this by converting DetailedExecutionInfo to sealed class?
        }

    private fun TransactionInfo.toDomainTransactionInfo(): DomainTransactionInfo =
        when (this) {
            is TransactionInfo.Custom ->
                DomainTransactionInfo.Custom(
                    to = to,
                    dataSize = dataSize,
                    value = value
                )
            is TransactionInfo.SettingsChange ->
                DomainTransactionInfo.SettingsChange(
                    dataDecoded = dataDecoded
                )
            is TransactionInfo.Transfer ->
                DomainTransactionInfo.Transfer(
                    sender = sender,
                    recipient = recipient,
                    transferInfo = transferInfo.toDomainTransferInfo(),
                    direction = direction
                )

            else -> throw IllegalStateException() // TODO make sealed class get rid of else branch
        }

    private fun TxData.toDomainTxData(): DomainTxData? =
        DomainTxData(
            hexData = hexData,
            dataDecoded = dataDecoded,
            to = to,
            value = value,
            operation = operation
        )

    private fun TransferInfo.toDomainTransferInfo(): DomainTransferInfo =
        when (this) {
            is Erc20Transfer -> {
                DomainTransferInfo.DomainErc20Transfer(
                    tokenAddress = tokenAddress,
                    value = value,
                    decimals = decimals,
                    logoUri = logoUri,
                    tokenName = tokenName,
                    tokenSymbol = tokenSymbol
                )
            }
            is Erc721Transfer -> DomainTransferInfo.DomainErc721Transfer(
                tokenAddress = tokenAddress,
                tokenSymbol = tokenSymbol,
                tokenName = tokenName,
                logoUri = logoUri,
                tokenId = tokenId
            )
            is EtherTransfer -> DomainTransferInfo.DomainEtherTransfer(
                value = value
            )
            else -> DomainTransferInfo.DomainEtherTransfer( // TODO make sealed class get rid of else branch
                value = "0"
            )
        }

    private fun List<Confirmations>.toDomainConfirmations(): List<DomainConfirmations> =
        this.map { confirmation ->
            DomainConfirmations(
                signer = confirmation.signer,
                signature = confirmation.signature
            )
        }

    private fun GateTransactionDto.toTransaction(): Transaction {
        return when (txInfo) {
            is TransactionInfo.Transfer -> Transaction.Transfer(
                id = id,
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce,
                date = timestamp.toDate(),
                recipient = txInfo.recipient,
                sender = txInfo.sender,
                value = txInfo.transferInfo.value().toBigInteger(),
                tokenInfo = txInfo.transferInfo.tokenInfo(),
                incoming = txInfo.direction == TransactionDirection.INCOMING
            )
            is TransactionInfo.SettingsChange -> Transaction.SettingsChange(
                id = id,
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce ?: BigInteger.ZERO,
                date = timestamp.toDate(),
                dataDecoded = txInfo.dataDecoded
            )
            is TransactionInfo.Custom -> Transaction.Custom(
                id = id,
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce,
                date = timestamp.toDate(),
                address = txInfo.to,
                dataSize = txInfo.dataSize,
                value = txInfo.value.toBigInteger()
            )
            is TransactionInfo.Creation -> Transaction.Creation(
                id = id,
                confirmations = null,
                status = TransactionStatus.SUCCESS
            )
            is TransactionInfo.Unknown -> Transaction.Custom(
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

    private fun TransferInfo.value(): String =
        when (this) {
            is Erc20Transfer -> value
            is Erc721Transfer -> "1"
            is EtherTransfer -> value
            else -> "0"
        }

    private fun TransferInfo.tokenInfo(): ServiceTokenInfo? =
        when (this) {
            is Erc20Transfer -> ServiceTokenInfo(
                address = tokenAddress,
                decimals = decimals ?: 0,
                symbol = tokenSymbol.orEmpty(),
                name = tokenName.orEmpty(),
                logoUri = logoUri,
                type = ServiceTokenInfo.TokenType.ERC20
            )
            is Erc721Transfer -> ServiceTokenInfo(
                address = tokenAddress,
                symbol = tokenSymbol.orEmpty(),
                name = tokenName.orEmpty(),
                logoUri = logoUri,
                type = ServiceTokenInfo.TokenType.ERC721
            )
            is EtherTransfer -> ETH_SERVICE_TOKEN_INFO
            else -> null
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

