package io.gnosis.safe.utils

import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.models.TransactionInfo
import io.gnosis.data.models.TransferInfo
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.getValueByName
import io.gnosis.safe.R
import io.gnosis.safe.ui.transactions.details.view.ActionInfoItem
import io.gnosis.safe.ui.transactions.getVersionForAddress
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

fun TransactionInfo.formattedAmount(balanceFormatter: BalanceFormatter): String =
    when (val txInfo = this) {
        is TransactionInfo.Custom -> {
            balanceFormatter.formatAmount(txInfo.value, true, 18, "ETH")
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
            balanceFormatter.formatAmount(value, incoming, decimals, symbol)
        }
        is TransactionInfo.SettingsChange -> "0 ETH"
        is TransactionInfo.Creation -> "0 ETH"
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
        is TransactionInfo.Custom, is TransactionInfo.SettingsChange, is TransactionInfo.Creation, TransactionInfo.Unknown -> "local::ethereum"
    }

fun TransactionInfo.SettingsChange.txActionInfoItems(): List<ActionInfoItem> {
    val settingsMethodTitle = mapOf(
        SafeRepository.METHOD_ADD_OWNER_WITH_THRESHOLD to R.string.tx_details_add_owner,
        SafeRepository.METHOD_CHANGE_MASTER_COPY to R.string.tx_details_new_mastercopy,
        SafeRepository.METHOD_CHANGE_THRESHOLD to R.string.tx_details_change_required_confirmations,
        SafeRepository.METHOD_DISABLE_MODULE to R.string.tx_details_disable_module,
        SafeRepository.METHOD_ENABLE_MODULE to R.string.tx_details_enable_module,
        SafeRepository.METHOD_REMOVE_OWNER to R.string.tx_details_remove_owner,
        SafeRepository.METHOD_SET_FALLBACK_HANDLER to R.string.tx_details_set_fallback_handler,
        SafeRepository.METHOD_SWAP_OWNER to R.string.tx_details_remove_owner
    )
    val result = mutableListOf<ActionInfoItem>()
    val settingsChange = this

    val params = settingsChange.dataDecoded.parameters
    when (settingsChange.dataDecoded.method) {
        SafeRepository.METHOD_CHANGE_MASTER_COPY -> {
            val mainCopy = params.getValueByName("_masterCopy")?.asEthereumAddress()
            val label = mainCopy?.let { it.getVersionForAddress() } ?: ""

            result.add(
                ActionInfoItem.AddressWithLabel(
                    itemLabel = settingsMethodTitle[settingsChange.dataDecoded.method],
                    address = mainCopy,
                    addressLabel = label
                )
            )
        }
        SafeRepository.METHOD_CHANGE_THRESHOLD -> {
            val value = params.getValueByName("_threshold") ?: ""
            result.add(ActionInfoItem.Value(itemLabel = settingsMethodTitle[settingsChange.dataDecoded.method], value = value))
        }
        SafeRepository.METHOD_ADD_OWNER_WITH_THRESHOLD -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_ADD_OWNER_WITH_THRESHOLD],
                    params.getValueByName("owner")?.asEthereumAddress()
                )
            )
            result.add(ActionInfoItem.Value(settingsMethodTitle[SafeRepository.METHOD_CHANGE_THRESHOLD], params.getValueByName("_threshold")!!))
        }
        SafeRepository.METHOD_REMOVE_OWNER -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_REMOVE_OWNER],
                    params.getValueByName("owner")?.asEthereumAddress()
                )
            )
            result.add(ActionInfoItem.Value(settingsMethodTitle[SafeRepository.METHOD_CHANGE_THRESHOLD], params.getValueByName("_threshold")!!))
        }
        SafeRepository.METHOD_SET_FALLBACK_HANDLER -> {
            val fallbackHandler = params.getValueByName("handler")?.asEthereumAddress()
            val label =
                if (SafeRepository.DEFAULT_FALLBACK_HANDLER == fallbackHandler) {
                    R.string.tx_list_default_fallback_handler
                } else {
                    R.string.tx_list_default_fallback_handler_unknown
                }
            result.add(
                ActionInfoItem.AddressWithLabel(
                    itemLabel = settingsMethodTitle[SafeRepository.METHOD_SET_FALLBACK_HANDLER],
                    address = fallbackHandler,
                    addressLabelRes = label
                )
            )
        }
        SafeRepository.METHOD_SWAP_OWNER -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_REMOVE_OWNER],
                    params.getValueByName("oldOwner")?.asEthereumAddress()
                )
            )
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_ADD_OWNER_WITH_THRESHOLD],
                    params.getValueByName("newOwner")?.asEthereumAddress()
                )
            )
        }
        SafeRepository.METHOD_ENABLE_MODULE -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_ENABLE_MODULE],
                    params.getValueByName("module")?.asEthereumAddress()
                )
            )
        }
        SafeRepository.METHOD_DISABLE_MODULE -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_DISABLE_MODULE],
                    params.getValueByName("module")?.asEthereumAddress()
                )
            )
        }
    }

    return result
}
