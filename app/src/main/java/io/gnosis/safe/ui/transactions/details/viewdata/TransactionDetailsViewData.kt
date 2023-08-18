package io.gnosis.safe.ui.transactions.details.viewdata

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.safe.ui.transactions.AddressInfoData
import io.gnosis.data.adapters.SolidityAddressNullableParceler
import io.gnosis.data.adapters.SolidityAddressParceler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import java.util.*

@Parcelize
data class TransactionDetailsViewData(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfoViewData,
    val executedAt: Date?,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?,
    val canSign: Boolean,
    val canExecute: Boolean,
    val hasOwnerKey: Boolean,
    //FIXME: replace Owner type with OwnerViewData and remove @IgnoredOnParcel
    @IgnoredOnParcel
    val owners: List<Owner> = listOf()
) : Parcelable

sealed class TransactionInfoViewData(
    val type: TransactionType
): Parcelable {
    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class Custom(
        val to: Solidity.Address,
        val actionInfoAddressName: String? = null,
        val actionInfoAddressUri: String? = null,
        val statusTitle: String? = null,
        val statusIconUri: String? = null,
        val safeApp: Boolean = false,
        val dataSize: Int,
        val value: BigInteger,
        val methodName: String?
    ) : TransactionInfoViewData(TransactionType.Custom)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class SettingsChange(
        val dataDecoded: DataDecoded,
        val settingsInfo: SettingsInfoViewData?
    ) : TransactionInfoViewData(TransactionType.SettingsChange)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class Transfer(
        val address: Solidity.Address,
        val addressUri: String?,
        val addressName: String?,
        val transferInfo: TransferInfo,
        val direction: TransactionDirection
    ) : TransactionInfoViewData(TransactionType.Transfer)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    @TypeParceler<Solidity.Address?, SolidityAddressNullableParceler>
    data class Creation(
        val creator: Solidity.Address,
        val creatorInfo: AddressInfoData?,
        val transactionHash: String,
        val implementation: Solidity.Address?,
        val implementationInfo: AddressInfoData?,
        val factory: Solidity.Address?,
        val factoryInfo: AddressInfoData?
    ) : TransactionInfoViewData(TransactionType.Creation)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class Rejection(
        val to: Solidity.Address,
        val addressName: String?,
        val addressUri: String?
    ) : TransactionInfoViewData(TransactionType.Custom)

    @Parcelize
    object Unknown : TransactionInfoViewData(TransactionType.Unknown)
}

sealed class SettingsInfoViewData(
    val type: SettingsInfoType
): Parcelable {

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class SetFallbackHandler(
        val handler: Solidity.Address,
        val handlerInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.SET_FALLBACK_HANDLER)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class AddOwner(
        val owner: Solidity.Address,
        val ownerInfo: AddressInfoData?,
        val threshold: Long
    ) : SettingsInfoViewData(SettingsInfoType.ADD_OWNER)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class RemoveOwner(
        val owner: Solidity.Address,
        val ownerInfo: AddressInfoData?,
        val threshold: Long
    ) : SettingsInfoViewData(SettingsInfoType.REMOVE_OWNER)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class SwapOwner(
        val oldOwner: Solidity.Address,
        val oldOwnerInfo: AddressInfoData?,
        val newOwner: Solidity.Address,
        val newOwnerInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.SWAP_OWNER)

    @Parcelize
    data class ChangeThreshold(
        val threshold: Long
    ) : SettingsInfoViewData(SettingsInfoType.CHANGE_THRESHOLD)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class ChangeImplementation(
        val implementation: Solidity.Address,
        val implementationInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.CHANGE_IMPLEMENTATION)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class EnableModule(
        val module: Solidity.Address,
        val moduleInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.ENABLE_MODULE)

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class DisableModule(
        val module: Solidity.Address,
        val moduleInfo: AddressInfoData?
    ) : SettingsInfoViewData(SettingsInfoType.DISABLE_MODULE)
}

fun TransactionDetails.toTransactionDetailsViewData(
    safes: List<Safe>,
    canSign: Boolean,
    canExecute: Boolean,
    owners: List<Owner>,
    hasOwnerKey: Boolean
): TransactionDetailsViewData =
    TransactionDetailsViewData(
        txHash = txHash,
        txStatus = txStatus,
        txInfo = txInfo.toTransactionInfoViewData(
            safes = safes,
            safeAppInfo = safeAppInfo,
            owners = owners
        ),
        executedAt = executedAt,
        txData = txData,
        detailedExecutionInfo = detailedExecutionInfo,
        canSign = canSign,
        canExecute = canExecute,
        hasOwnerKey = hasOwnerKey,
        owners = owners
    )

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun TransactionInfo.toTransactionInfoViewData(
    safes: List<Safe>,
    safeAppInfo: SafeAppInfo? = null,
    owners: List<Owner> = emptyList()
): TransactionInfoViewData =
    when (this) {
        is TransactionInfo.Custom -> {
            val addressUri = when (val toInfo = to.toAddressInfoData(safes)) {
                is AddressInfoData.Remote -> toInfo.addressLogoUri
                else -> null
            }
            val addressName = when (val toInfo = to.toAddressInfoData(safes)) {
                is AddressInfoData.Local -> toInfo.name
                is AddressInfoData.Remote -> toInfo.name
                else -> null
            }
            if (isCancellation) {
                TransactionInfoViewData.Rejection(
                    to = to.value,
                    addressName = addressName,
                    addressUri = addressUri
                )
            } else {
                TransactionInfoViewData.Custom(
                    to = to.value,
                    actionInfoAddressName = addressName,
                    actionInfoAddressUri = addressUri,
                    statusTitle = safeAppInfo?.name,
                    statusIconUri = safeAppInfo?.logoUri,
                    dataSize = dataSize,
                    value = value,
                    methodName = methodName,
                    safeApp = safeAppInfo != null
                )
            }
        }
        is TransactionInfo.Creation -> TransactionInfoViewData.Creation(
            creator.value,
            AddressInfoData.Remote(creator.name, creator.logoUri, creator.value.asEthereumAddressString()),
            transactionHash,
            implementation?.value,
            AddressInfoData.Remote(implementation?.name, implementation?.logoUri, implementation?.value?.asEthereumAddressString()),
            factory?.value,
            AddressInfoData.Remote(factory?.name, factory?.logoUri, factory?.value?.asEthereumAddressString())
        )
        is TransactionInfo.SettingsChange -> TransactionInfoViewData.SettingsChange(
            dataDecoded,
            settingsInfo.toSettingsInfoViewData(safes, owners = owners)
        )
        is TransactionInfo.Transfer -> {
            val addressInfoData =
                if (direction == TransactionDirection.OUTGOING) {
                    recipient.toAddressInfoData(safes)
                } else {
                    sender.toAddressInfoData(safes)
                }

            val addressUri = when (addressInfoData) {
                is AddressInfoData.Remote -> addressInfoData.addressLogoUri
                else -> null
            }
            val addressName = when (addressInfoData) {
                is AddressInfoData.Local -> addressInfoData.name
                is AddressInfoData.Remote -> addressInfoData.name
                else -> null
            }
            TransactionInfoViewData.Transfer(
                address = if (direction == TransactionDirection.OUTGOING) recipient.value else sender.value,
                addressUri = addressUri,
                addressName = addressName,
                transferInfo = transferInfo,
                direction = direction
            )
        }
        is TransactionInfo.Unknown -> TransactionInfoViewData.Unknown
    }

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun SettingsInfo?.toSettingsInfoViewData(
    safes: List<Safe>,
    owners: List<Owner>,
    safeAppInfo: SafeAppInfo? = null
): SettingsInfoViewData? =
    when (this) {
        is SettingsInfo.SetFallbackHandler -> SettingsInfoViewData.SetFallbackHandler(
            handler.value,
            handler.toAddressInfoData(safes, safeAppInfo)
        )
        is SettingsInfo.AddOwner -> SettingsInfoViewData.AddOwner(owner.value, owner.toAddressInfoData(safes, safeAppInfo, owners), threshold)
        is SettingsInfo.RemoveOwner -> SettingsInfoViewData.RemoveOwner(
            owner.value,
            owner.toAddressInfoData(safes, safeAppInfo, owners),
            threshold
        )
        is SettingsInfo.SwapOwner -> SettingsInfoViewData.SwapOwner(
            oldOwner.value,
            oldOwner.toAddressInfoData(safes, safeAppInfo, owners),
            newOwner.value,
            newOwner.toAddressInfoData(safes, safeAppInfo, owners)
        )
        is SettingsInfo.ChangeThreshold -> SettingsInfoViewData.ChangeThreshold(threshold)
        is SettingsInfo.ChangeImplementation -> SettingsInfoViewData.ChangeImplementation(
            implementation.value,
            implementation.toAddressInfoData(safes, safeAppInfo)
        )
        is SettingsInfo.EnableModule -> SettingsInfoViewData.EnableModule(
            module.value, module.toAddressInfoData(
                safes,
                safeAppInfo
            )
        )
        is SettingsInfo.DisableModule -> SettingsInfoViewData.DisableModule(
            module.value, module.toAddressInfoData(
                safes,
                safeAppInfo
            )
        )
        null -> null
    }

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun AddressInfo.toAddressInfoData(
    safes: List<Safe>,
    safeAppInfo: SafeAppInfo? = null,
    owners: List<Owner> = emptyList()
): AddressInfoData {
    val localSafeName = safes.find { it.address == value }?.localName
    val localOwnerName = owners.find { it.address == value }?.name

    val addressString = value.asEthereumAddressChecksumString()

    return when {
        localSafeName != null -> AddressInfoData.Local(localSafeName, addressString)
        localOwnerName != null -> AddressInfoData.Local(localOwnerName, addressString)
        safeAppInfo != null -> AddressInfoData.Remote(safeAppInfo.name, safeAppInfo.logoUri, addressString)
        !this.name.isNullOrBlank() -> AddressInfoData.Remote(this.name, this.logoUri, addressString)
        else -> AddressInfoData.Default
    }
}


