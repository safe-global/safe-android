package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import io.gnosis.data.models.transaction.*
import pm.gnosis.common.adapters.moshi.*

internal val transferInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransferInfo::class.java, "type")
        .withSubtype(TransferInfo.Erc20Transfer::class.java, "ERC20")
        .withSubtype(TransferInfo.Erc721Transfer::class.java, "ERC721")
        .withSubtype(TransferInfo.NativeTransfer::class.java, "NATIVE_COIN")

internal val transactionInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransactionInfo::class.java, "type")
        .withSubtype(TransactionInfo.Transfer::class.java, "Transfer")
        .withSubtype(TransactionInfo.SettingsChange::class.java, "SettingsChange")
        .withSubtype(TransactionInfo.Custom::class.java, "Custom")
        .withSubtype(TransactionInfo.SwapOrder::class.java, "SwapOrder")
        .withSubtype(TransactionInfo.SwapTransfer::class.java, "SwapTransfer")
        .withSubtype(TransactionInfo.TwapOrder::class.java, "TwapOrder")
        .withSubtype(TransactionInfo.Creation::class.java, "Creation")
        .withDefaultValue(TransactionInfo.Unknown)

internal val transactionExecutionDetailsAdapter =
    PolymorphicJsonAdapterFactory.of(DetailedExecutionInfo::class.java, "type")
        .withSubtype(DetailedExecutionInfo.MultisigExecutionDetails::class.java, "MULTISIG")
        .withSubtype(DetailedExecutionInfo.ModuleExecutionDetails::class.java, "MODULE")

internal val settingsInfoAdapter =
    PolymorphicJsonAdapterFactory.of(SettingsInfo::class.java, "type")
        .withSubtype(SettingsInfo.SetFallbackHandler::class.java, "SET_FALLBACK_HANDLER")
        .withSubtype(SettingsInfo.AddOwner::class.java, "ADD_OWNER")
        .withSubtype(SettingsInfo.RemoveOwner::class.java, "REMOVE_OWNER")
        .withSubtype(SettingsInfo.SwapOwner::class.java, "SWAP_OWNER")
        .withSubtype(SettingsInfo.ChangeThreshold::class.java, "CHANGE_THRESHOLD")
        .withSubtype(SettingsInfo.ChangeImplementation::class.java, "CHANGE_IMPLEMENTATION")
        .withSubtype(SettingsInfo.EnableModule::class.java, "ENABLE_MODULE")
        .withSubtype(SettingsInfo.DisableModule::class.java, "DISABLE_MODULE")

internal val txListEntryAdapter =
    PolymorphicJsonAdapterFactory.of(TxListEntry::class.java, "type")
        .withSubtype(TxListEntry.Transaction::class.java, "TRANSACTION")
        .withSubtype(TxListEntry.DateLabel::class.java, "DATE_LABEL")
        .withSubtype(TxListEntry.Label::class.java, "LABEL")
        .withSubtype(TxListEntry.ConflictHeader::class.java, "CONFLICT_HEADER")
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
        .build()
