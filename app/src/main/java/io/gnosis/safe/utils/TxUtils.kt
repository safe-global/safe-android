package io.gnosis.safe.utils

import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.models.TransactionInfo
import io.gnosis.data.models.TransferInfo
import java.math.BigInteger

fun TransactionInfo.formattedAmount(): String? =
    when (val txInfo = this) {
        is TransactionInfo.Custom -> {
            txInfo.value.formatAmount(true, 18, "ETH")
        }
        is TransactionInfo.Transfer -> {
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
            value.formatAmount(incoming, decimals, symbol)
        }
        is TransactionInfo.SettingsChange -> "0 ETH"
        TransactionInfo.Creation -> "0 ETH"
        TransactionInfo.Unknown -> "0 ETH"
    }

fun TransactionInfo.logoUri(): String? =
    when (val transactionInfo = this) {
        is TransactionInfo.Transfer -> when (val transferInfo = transactionInfo.transferInfo) {
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
        is TransactionInfo.Custom, is TransactionInfo.SettingsChange, TransactionInfo.Creation, TransactionInfo.Unknown -> "local::ethereum"
    }
