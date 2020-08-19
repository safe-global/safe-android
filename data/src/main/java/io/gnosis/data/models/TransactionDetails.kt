package io.gnosis.data.models

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.Operation
import io.gnosis.data.backend.dto.TransactionDirection
import pm.gnosis.model.Solidity
import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*

data class TransactionDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfo,
    val executedAt: Date?,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?
)

data class TxData(
    val hexData: String?,
    val dataDecoded: DataDecodedDto?,
    val to: Solidity.Address,
    val value: BigInteger?,
    val operation: Operation
)

sealed class DetailedExecutionInfo {
    data class MultisigExecutionDetails(
        val submittedAt: Date,
        val nonce: BigInteger,
        val safeTxHash: String,
        val signers: List<Solidity.Address>,
        val confirmationsRequired: Int,
        val confirmations: List<Confirmations>,
        val executor: Solidity.Address?
    ) : DetailedExecutionInfo()

    data class ModuleExecutionDetails(
        val address: String
    ) : DetailedExecutionInfo()
}

sealed class TransactionInfo {
    fun formattedAmount(): String? =
        when (val txInfo = this) {
            is Custom -> {
                txInfo.value.formatAmount(true, 18, "ETH")
            }
            is Transfer -> {
                val incoming = txInfo.direction == TransactionDirection.INCOMING
                val decimals: Int = when (val transferInfo = txInfo.transferInfo) {
                    is TransferInfo.Erc20Transfer -> {
                        transferInfo.decimals ?: 0
                    }
                    is TransferInfo.EtherTransfer -> 18
                    else -> 0
                }
                val symbol: String = when (val transferInfo = txInfo.transferInfo) {
                    is TransferInfo.Erc20Transfer -> {
                        transferInfo.tokenSymbol ?: ""
                    }
                    is TransferInfo.Erc721Transfer -> {
                        transferInfo.tokenSymbol ?: ""
                    }
                    else -> {
                        "ETH"
                    }
                }
                val value = when (val transferInfo = txInfo.transferInfo) {
                    is TransferInfo.Erc20Transfer -> {
                        transferInfo.value
                    }
                    is TransferInfo.Erc721Transfer -> {
                        BigInteger.ONE
                    }

                    is TransferInfo.EtherTransfer -> {
                        transferInfo.value
                    }

                }
                value.formatAmount(incoming, decimals, symbol) ?: ""
            }
            is SettingsChange -> "0 ETH"
            Creation -> "0 ETH"
            Unknown -> "0 ETH"
        }

    fun logoUri(): String? =
        when (val transactionInfo = this) {
            is Transfer -> when (val transferInfo = transactionInfo.transferInfo) {
                is TransferInfo.Erc20Transfer -> {
                    transferInfo.logoUri
                }
                is TransferInfo.Erc721Transfer -> {
                    transferInfo.logoUri
                }
                else -> {
                    "local::ethereum"
                }
            }
            is Custom, is SettingsChange, Creation, Unknown -> "local::ethereum"
        }

    data class Custom(
        val to: Solidity.Address,
        val dataSize: Int,
        val value: BigInteger
    ) : TransactionInfo()

    data class SettingsChange(
        val dataDecoded: DataDecodedDto
    ) : TransactionInfo()

    data class Transfer(
        val sender: Solidity.Address,
        val recipient: Solidity.Address,
        val transferInfo: TransferInfo,
        val direction: TransactionDirection
    ) : TransactionInfo()

    object Creation : TransactionInfo()

    object Unknown : TransactionInfo()
}

sealed class TransferInfo {
    data class Erc20Transfer(
        val tokenAddress: Solidity.Address,
        val tokenName: String?,
        val tokenSymbol: String?,
        val logoUri: String?,
        val decimals: Int?,
        val value: BigInteger
    ) : TransferInfo()

    data class Erc721Transfer(
        val tokenAddress: Solidity.Address,
        val tokenId: String,
        val tokenName: String?,
        val tokenSymbol: String?,
        val logoUri: String?
    ) : TransferInfo()

    data class EtherTransfer(
        val value: BigInteger
    ) : TransferInfo()
}

data class Confirmations(
    val signer: Solidity.Address,
    val signature: String
)

fun BigInteger.formatAmount(incoming: Boolean, decimals: Int = 18, symbol: String = "ETH"): String {
    val inOut = if (this == BigInteger.ZERO) "" else if (incoming) "+" else "-"
    val decimalValue = this.shiftedString(decimals = decimals)
    return "%s%s %s".format(inOut, decimalValue, symbol)
}

fun BigInteger.shiftedString(decimals: Int, decimalsToDisplay: Int = 5, roundingMode: RoundingMode = RoundingMode.DOWN) =
    shifted(decimals, decimalsToDisplay, roundingMode).stringWithNoTrailingZeroes()

fun BigInteger.convertAmount(decimals: Int): BigDecimal =
    BigDecimal(this).setScale(decimals).div(BigDecimal.TEN.pow(decimals))

fun BigInteger.shifted(decimals: Int, decimalsToDisplay: Int = 5, roundingMode: RoundingMode = RoundingMode.DOWN): BigDecimal =
    convertAmount(decimals).setScale(decimalsToDisplay, roundingMode)
