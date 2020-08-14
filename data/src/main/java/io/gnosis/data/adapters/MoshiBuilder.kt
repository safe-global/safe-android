package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.backend.dto.DetailedExecutionInfoDto
import io.gnosis.data.backend.dto.DetailedExecutionInfoType
import io.gnosis.data.backend.dto.GateTransactionType
import io.gnosis.data.backend.dto.GateTransferType
import io.gnosis.data.backend.dto.TransactionInfoDto
import io.gnosis.data.backend.dto.TransferInfoDto
import pm.gnosis.common.adapters.moshi.BigDecimalNumberAdapter
import pm.gnosis.common.adapters.moshi.DecimalNumberAdapter
import pm.gnosis.common.adapters.moshi.DefaultNumberAdapter
import pm.gnosis.common.adapters.moshi.HexNumberAdapter
import pm.gnosis.common.adapters.moshi.SolidityAddressAdapter
import pm.gnosis.common.adapters.moshi.WeiAdapter
import java.util.*

internal val transferInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransferInfoDto::class.java, TransferInfoDto::type::name.get())
        .withSubtype(TransferInfoDto.Erc20Transfer::class.java, GateTransferType.ERC20.name)
        .withSubtype(TransferInfoDto.Erc721Transfer::class.java, GateTransferType.ERC721.name)
        .withSubtype(TransferInfoDto.EtherTransfer::class.java, GateTransferType.ETHER.name)

internal val transactionInfoAdapter =
    PolymorphicJsonAdapterFactory.of(TransactionInfoDto::class.java, TransactionInfoDto::type::name.get())
        .withSubtype(TransactionInfoDto.Transfer::class.java, GateTransactionType.Transfer.name)
        .withSubtype(TransactionInfoDto.SettingsChange::class.java, GateTransactionType.SettingsChange.name)
        .withSubtype(TransactionInfoDto.Custom::class.java, GateTransactionType.Custom.name)
        .withSubtype(TransactionInfoDto.Creation::class.java, GateTransactionType.Creation.name)
        .withDefaultValue(TransactionInfoDto.Unknown())

internal val transactionExecutionDetailsAdapter =
    PolymorphicJsonAdapterFactory.of(DetailedExecutionInfoDto::class.java, DetailedExecutionInfoDto::type::name.get())
        .withSubtype(DetailedExecutionInfoDto.MultisigExecutionDetailsDto::class.java, DetailedExecutionInfoType.MULTISIG.name)
        .withSubtype(DetailedExecutionInfoDto.ModuleExecutionDetailsDto::class.java, DetailedExecutionInfoType.MODULE.name)

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
        .add(transactionExecutionDetailsAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()
