package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.models.transaction.*
import pm.gnosis.common.adapters.moshi.*

internal val transferInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransferInfo::class.java, TransferInfo::type::name.get())
        .withSubtype(TransferInfo.Erc20Transfer::class.java, TransferType.ERC20.name)
        .withSubtype(TransferInfo.Erc721Transfer::class.java, TransferType.ERC721.name)
        .withSubtype(TransferInfo.EtherTransfer::class.java, TransferType.ETHER.name)

internal val transactionInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransactionInfo::class.java, TransactionInfo::type::name.get())
        .withSubtype(TransactionInfo.Transfer::class.java, TransactionType.Transfer.name)
        .withSubtype(TransactionInfo.SettingsChange::class.java, TransactionType.SettingsChange.name)
        .withSubtype(TransactionInfo.Custom::class.java, TransactionType.Custom.name)
        .withSubtype(TransactionInfo.Creation::class.java, TransactionType.Creation.name)
        .withDefaultValue(TransactionInfo.Unknown)

internal val transactionExecutionDetailsAdapter =
    PolymorphicJsonAdapterFactory.of(DetailedExecutionInfo::class.java, DetailedExecutionInfo::type::name.get())
        .withSubtype(DetailedExecutionInfo.MultisigExecutionDetails::class.java, DetailedExecutionInfoType.MULTISIG.name)
        .withSubtype(DetailedExecutionInfo.ModuleExecutionDetails::class.java, DetailedExecutionInfoType.MODULE.name)

internal val settingsInfoAdapter =
    PolymorphicJsonAdapterFactory.of(SettingsInfo::class.java, SettingsInfo::type::name.get())
        .withSubtype(SettingsInfo.SetFallbackHandler::class.java, SettingsInfoType.SET_FALLBACK_HANDLER.name)
        .withSubtype(SettingsInfo.AddOwner::class.java, SettingsInfoType.ADD_OWNER.name)
        .withSubtype(SettingsInfo.RemoveOwner::class.java, SettingsInfoType.REMOVE_OWNER.name)
        .withSubtype(SettingsInfo.SwapOwner::class.java, SettingsInfoType.SWAP_OWNER.name)
        .withSubtype(SettingsInfo.ChangeThreshold::class.java, SettingsInfoType.CHANGE_THRESHOLD.name)
        .withSubtype(SettingsInfo.ChangeImplementation::class.java, SettingsInfoType.CHANGE_IMPLEMENTATION.name)
        .withSubtype(SettingsInfo.EnableModule::class.java, SettingsInfoType.ENABLE_MODULE.name)
        .withSubtype(SettingsInfo.DisableModule::class.java, SettingsInfoType.DISABLE_MODULE.name)

internal val txListEntryAdapter =
    PolymorphicJsonAdapterFactory.of(TxListEntry::class.java, "type")
        .withSubtype(TxListEntry.Transaction::class.java, TxListEntryType.TRANSACTION.name)
        .withSubtype(TxListEntry.DateLabel::class.java, TxListEntryType.DATE_LABEL.name)
        .withSubtype(TxListEntry.Label::class.java, TxListEntryType.LABEL.name)
        .withSubtype(TxListEntry.ConflictHeader::class.java, TxListEntryType.CONFLICT_HEADER.name)
        .withDefaultValue(TxListEntry.Unknown)

val dataMoshi =
    Moshi.Builder()
        .add(DateAdapter())
        .add(WeiAdapter())
        .add(DecimalNumberAdapter())
        .add(BigDecimalNumberAdapter())
        .add(HexNumberAdapter())
        .add(DefaultNumberAdapter())
        .add(SolidityAddressAdapter())
        .add(OperationEnumAdapter())
        .add(TokenTypeAdapter())
        .add(settingsInfoAdapter)
        .add(transferInfoAdapter)
        .add(transactionInfoAdapter)
        .add(transactionExecutionDetailsAdapter)
        .add(txListEntryAdapter)
        .add(ParamAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
