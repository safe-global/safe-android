package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.backend.dto.*
import pm.gnosis.common.adapters.moshi.*

internal val transferInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransferInfo::class.java, TransferInfo::type::name.get())
        .withSubtype(Erc20Transfer::class.java, GateTransferType.ERC20.name)
        .withSubtype(Erc721Transfer::class.java, GateTransferType.ERC721.name)
        .withSubtype(EtherTransfer::class.java, GateTransferType.ETHER.name)

internal val transactionInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransactionInfo::class.java, TransactionInfo::type::name.get())
        .withSubtype(Transfer::class.java, GateTransactionType.Transfer.name)
        .withSubtype(SettingsChange::class.java, GateTransactionType.SettingsChange.name)
        .withSubtype(Custom::class.java, GateTransactionType.Custom.name)

val dataMoshi =
    Moshi.Builder()
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
