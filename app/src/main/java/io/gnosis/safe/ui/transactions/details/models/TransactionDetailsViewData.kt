package io.gnosis.safe.ui.transactions.details.models

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.safe.ui.transactions.AddressInfoData
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import java.util.*

class TransactionDetailsViewData(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfoViewData,
    val executedAt: Date?,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?
)

sealed class TransactionInfoViewData(
    val type: TransactionType
) {
    data class Custom(
        val to: Solidity.Address,
        val toInfo: AddressInfoData?,
        val dataSize: Int,
        val value: BigInteger,
        val methodName: String?
    ) : TransactionInfoViewData(TransactionType.Custom)

    data class SettingsChange(
        val dataDecoded: DataDecoded,
        val settingsInfo: SettingsInfoViewData?
    ) : TransactionInfoViewData(TransactionType.SettingsChange)

    data class Transfer(
        val sender: Solidity.Address,
        val senderInfo: AddressInfoData?,
        val recipient: Solidity.Address,
        val recipientInfo: AddressInfoData?,
        val transferInfo: TransferInfo,
        val direction: TransactionDirection
    ) : TransactionInfoViewData(TransactionType.Transfer)

    data class Creation(
        val creator: Solidity.Address,
        val transactionHash: String,
        val implementation: Solidity.Address?,
        val factory: Solidity.Address?
    ) : TransactionInfoViewData(TransactionType.Creation)

    object Unknown : TransactionInfoViewData(TransactionType.Unknown)
}

sealed class SettingsInfoViewData(
    val type: SettingsInfoType
) {
    data class SetFallbackHandler(
        val handler: Solidity.Address,
        val handlerInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.SET_FALLBACK_HANDLER)

    data class AddOwner(
        val owner: Solidity.Address,
        val ownerInfo: AddressInfoData?,
        val threshold: Long
    ) : SettingsInfoViewData(SettingsInfoType.ADD_OWNER)

    data class RemoveOwner(
        val owner: Solidity.Address,
        val ownerInfo: AddressInfoData?,
        val threshold: Long
    ) : SettingsInfoViewData(SettingsInfoType.REMOVE_OWNER)

    data class SwapOwner(
        val oldOwner: Solidity.Address,
        val oldOwnerInfo: AddressInfoData?,
        val newOwner: Solidity.Address,
        val newOwnerInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.SWAP_OWNER)

    data class ChangeThreshold(
        val threshold: Long
    ) : SettingsInfoViewData(SettingsInfoType.CHANGE_THRESHOLD)

    data class ChangeImplementation(
        val implementation: Solidity.Address,
        val implementationInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.CHANGE_IMPLEMENTATION)

    data class EnableModule(
        val module: Solidity.Address,
        val moduleInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.ENABLE_MODULE)

    data class DisableModule(
        val module: Solidity.Address,
        val moduleInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.DISABLE_MODULE)
}

fun TransactionDetails.toTransactionDetailsViewData(safes: List<Safe>): TransactionDetailsViewData =
    TransactionDetailsViewData(txHash, txStatus, txInfo.toTransactionInfoViewData(safes), executedAt, txData, detailedExecutionInfo)

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun TransactionInfo.toTransactionInfoViewData(safes: List<Safe>): TransactionInfoViewData =
    when (this) {
        is TransactionInfo.Custom -> {

            TransactionInfoViewData.Custom(to, toInfo.toAddressInfoData(to, safes), dataSize, value, methodName)
        }
        is TransactionInfo.Creation -> TransactionInfoViewData.Creation(creator, transactionHash, implementation, factory)
        is TransactionInfo.SettingsChange -> TransactionInfoViewData.SettingsChange(
            dataDecoded,
            settingsInfo.toSettingsInfoViewData(safes)
        )
        is TransactionInfo.Transfer -> TransactionInfoViewData.Transfer(
            sender,
            senderInfo.toAddressInfoData(sender, safes),
            recipient,
            recipientInfo.toAddressInfoData(recipient, safes),
            transferInfo,
            direction
        )
        is TransactionInfo.Unknown -> TransactionInfoViewData.Unknown
    }

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun SettingsInfo?.toSettingsInfoViewData(safes: List<Safe>): SettingsInfoViewData? =
    when (this) {
        is SettingsInfo.SetFallbackHandler -> SettingsInfoViewData.SetFallbackHandler(handler, handlerInfo.toAddressInfoData(handler, safes))
        is SettingsInfo.AddOwner -> SettingsInfoViewData.AddOwner(owner, ownerInfo.toAddressInfoData(owner, safes), threshold)
        is SettingsInfo.RemoveOwner -> SettingsInfoViewData.RemoveOwner(owner, ownerInfo.toAddressInfoData(owner, safes), threshold)
        is SettingsInfo.SwapOwner -> SettingsInfoViewData.SwapOwner(
            oldOwner,
            oldOwnerInfo.toAddressInfoData(oldOwner, safes),
            newOwner,
            newOwnerInfo.toAddressInfoData(newOwner, safes)
        )
        is SettingsInfo.ChangeThreshold -> SettingsInfoViewData.ChangeThreshold(threshold)
        is SettingsInfo.ChangeImplementation -> SettingsInfoViewData.ChangeImplementation(
            implementation,
            implementationInfo.toAddressInfoData(implementation, safes)
        )
        is SettingsInfo.EnableModule -> SettingsInfoViewData.EnableModule(module, moduleInfo.toAddressInfoData(module, safes))
        is SettingsInfo.DisableModule -> SettingsInfoViewData.DisableModule(module, moduleInfo.toAddressInfoData(module, safes))
        null -> null
    }

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun AddressInfo?.toAddressInfoData(address: Solidity.Address, safes: List<Safe>): AddressInfoData? {
    val localName = safes.find { it.address == address }?.localName
    val addressString = address.asEthereumAddressString()
    return when {
        localName != null -> AddressInfoData.Local(localName, addressString)
        this != null -> AddressInfoData.Remote(this.name, this.logoUri, addressString)
        else -> AddressInfoData.Default
    }
}


