package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.backend.dto.Creation
import io.gnosis.data.backend.dto.Custom
import io.gnosis.data.backend.dto.Erc20Transfer
import io.gnosis.data.backend.dto.Erc721Transfer
import io.gnosis.data.backend.dto.EtherTransfer
import io.gnosis.data.backend.dto.GateTransactionType
import io.gnosis.data.backend.dto.GateTransferType
import io.gnosis.data.backend.dto.SettingsChange
import io.gnosis.data.backend.dto.TransactionInfo
import io.gnosis.data.backend.dto.Transfer
import io.gnosis.data.backend.dto.TransferInfo
import pm.gnosis.common.adapters.moshi.BigDecimalNumberAdapter
import pm.gnosis.common.adapters.moshi.DecimalNumberAdapter
import pm.gnosis.common.adapters.moshi.DefaultNumberAdapter
import pm.gnosis.common.adapters.moshi.HexNumberAdapter
import pm.gnosis.common.adapters.moshi.SolidityAddressAdapter
import pm.gnosis.common.adapters.moshi.WeiAdapter
import java.util.*

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
        .withSubtype(Creation::class.java, GateTransactionType.Creation.name)

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
