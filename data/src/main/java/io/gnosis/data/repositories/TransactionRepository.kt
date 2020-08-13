package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.dto.Erc20Transfer
import io.gnosis.data.backend.dto.Erc721Transfer
import io.gnosis.data.backend.dto.EtherTransfer
import io.gnosis.data.backend.dto.GateTransactionDto
import io.gnosis.data.backend.dto.MultisigExecutionDetails
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.backend.dto.TransactionInfo
import io.gnosis.data.backend.dto.TransferInfo
import io.gnosis.data.models.CreationDetails
import io.gnosis.data.models.CustomDetails
import io.gnosis.data.models.Page
import io.gnosis.data.models.SettingsChangeDetails
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.TransactionDetails
import io.gnosis.data.models.TransactionStatus
import io.gnosis.data.models.TransferDetails
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
            when (it.txInfo) {
                is TransactionInfo.Transfer -> {
                    TransferDetails(
                        txHash = it.txHash,
                        txStatus = it.txStatus,
                        createdAt = (it.detailedExecutionInfo as? MultisigExecutionDetails)?.submittedAt?.toDate(),
                        executedAt = it.executedAt?.toDate(),
                        executor = (it.txInfo as? TransactionInfo.Transfer)?.sender!!, // TODO: handle other transfer type
                        txData = it.txData,
                        detailedExecutionInfo = it.detailedExecutionInfo,
                        incoming = (it.txInfo as? TransactionInfo.Transfer)?.direction == TransactionDirection.INCOMING
                    )
                }
                is TransactionInfo.Custom -> {
                    CustomDetails(
                        txHash = it.txHash,
                        executedAt = it.executedAt?.let { date ->
                            Date(date)
                        },
                        createdAt = (it.detailedExecutionInfo as MultisigExecutionDetails).submittedAt?.let { date ->
                            Date(date)
                        },
                        detailedExecutionInfo = it.detailedExecutionInfo,
                        txStatus = it.txStatus,
                        txData = it.txData,
                        executor = Solidity.Address(BigInteger.ZERO)  // TODO: handle other transfer type
                    )
                }
                is TransactionInfo.SettingsChange -> {
                    SettingsChangeDetails(
                        txHash = it.txHash,
                        txData = it.txData,
                        txStatus = it.txStatus,
                        detailedExecutionInfo = it.detailedExecutionInfo,
                        createdAt = (it.detailedExecutionInfo as MultisigExecutionDetails).submittedAt?.let { date ->
                            Date(date)
                        },
                        executedAt = it.executedAt?.let { date ->
                            Date(date)
                        },
                        executor = Solidity.Address(BigInteger.ZERO) // TODO: handle other transfer type

                    )
                }
                is TransactionInfo.Creation -> {
                    CreationDetails(
                        txHash = it.txHash,
                        txData = it.txData,
                        txStatus = it.txStatus,
                        createdAt = null,
                        detailedExecutionInfo = null,
                        executedAt = null,
                        executor = Solidity.Address(BigInteger.ZERO)// TODO: handle other transfer type
                    )
                }
                is TransactionInfo.Unknown -> {
                    CustomDetails(
                        txHash = it.txHash,
                        txData = it.txData,
                        txStatus = it.txStatus,
                        executedAt = null,
                        detailedExecutionInfo = null,
                        createdAt = null,
                        executor = Solidity.Address(BigInteger.ZERO)
                    )
                }
            }
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
                dataSize = txInfo.dataSize.toLong(),
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
                dataSize = 0L,
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
