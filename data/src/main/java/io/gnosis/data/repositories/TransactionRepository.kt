package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.dto.Custom
import io.gnosis.data.backend.dto.Erc20Transfer
import io.gnosis.data.backend.dto.Erc721Transfer
import io.gnosis.data.backend.dto.EtherTransfer
import io.gnosis.data.backend.dto.GateTransactionDto
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.backend.dto.SettingsChange
import io.gnosis.data.backend.dto.Transfer
import io.gnosis.data.backend.dto.TransferInfo
import io.gnosis.data.models.Page
import io.gnosis.data.models.Transaction
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger
import java.text.SimpleDateFormat
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

    private fun GateTransactionDto.toTransaction(): Transaction {
        return when (txInfo) {
            is Transfer -> Transaction.Transfer(
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce?.toBigInteger(),
                date = timestamp.toDate(),
                recipient = txInfo.recipient,
                sender = txInfo.sender,
                value = txInfo.transferInfo.value().toBigInteger(),
                tokenInfo = txInfo.transferInfo.tokenInfo()
            )
            is SettingsChange -> Transaction.SettingsChange(
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce?.toBigInteger() ?: BigInteger.ZERO,
                date = timestamp.toDate(),
                dataDecoded = txInfo.dataDecoded
            )
            is Custom -> Transaction.Custom(
                status = txStatus,
                confirmations = executionInfo?.confirmationsSubmitted,
                nonce = executionInfo?.nonce?.toBigInteger(),
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
                type = ServiceTokenInfo.TokenType.ERC20
            )
            else -> null
        }

    private fun Long.toDate(): String =
        with(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")) {
            format(Date().apply { time = this@toDate })
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
