package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.dto.*
import io.gnosis.data.models.Page
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.TransactionDetails
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
        gatewayApi.loadTransactionDetails(txId).let {
            TransactionDetails(
                it.txHash,
                it.txStatus,
                it.detailedExecutionInfo.sumbittedAt?.toDate(),
                it.executedAt?.toDate(),
                it.txInfo.sender,
                it.txData,
                it.detailedExecutionInfo
            )
        }

    private fun GateTransactionDto.toTransaction(): Transaction {
        return when (txInfo) {
            is Transfer -> Transaction.Transfer(
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
            is SettingsChange -> Transaction.SettingsChange(
                id = id,
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce ?: BigInteger.ZERO,
                date = timestamp.toDate(),
                dataDecoded = txInfo.dataDecoded
            )
            is Custom -> Transaction.Custom(
                id = id,
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce,
                date = timestamp.toDate(),
                address = txInfo.to,
                dataSize = txInfo.dataSize.toLong(),
                value = txInfo.value.toBigInteger()
            )
            else -> throw IllegalStateException()
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
