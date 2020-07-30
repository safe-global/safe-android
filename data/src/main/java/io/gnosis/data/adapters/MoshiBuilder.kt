package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.backend.dto.*
import pm.gnosis.common.adapters.moshi.*
import java.util.*

internal val transferInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransferInfo::class.java, TransferInfo::type::name.get())
        .withSubtype(Erc20Transfer::class.java, TransferType.ERC20_TRANSFER.name)
        .withSubtype(Erc721Transfer::class.java, TransferType.ERC721_TRANSFER.name)
        .withSubtype(EtherTransfer::class.java, TransferType.ETHER_TRANSFER.name)

internal val transactionInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransactionInfo::class.java, TransactionInfo::type::name.get())
        .withSubtype(Transfer::class.java, GateTransactionType.Transfer.name)
        .withSubtype(SettingsChange::class.java, GateTransactionType.SettingsChange.name)
        .withSubtype(Custom::class.java, GateTransactionType.Custom.name)

val dataMoshi =
    Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter())
        .add(WeiAdapter())
        .add(DecimalNumberAdapter())
        .add(BigDecimalNumberAdapter())
        .add(HexNumberAdapter())
        .add(DefaultNumberAdapter())
        .add(SolidityAddressAdapter())
        .add(OperationEnumAdapter())
        .add(transferInfoAdapter)
        .add(transactionInfoAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()
