package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.models.transaction.*
import pm.gnosis.common.adapters.moshi.*
import java.util.*

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
        .add(transferInfoAdapter)
        .add(transactionInfoAdapter)
        .add(transactionExecutionDetailsAdapter)
        .add(ParamAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
