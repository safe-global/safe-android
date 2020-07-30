package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.backend.dto.*
import pm.gnosis.common.adapters.moshi.*

internal val transactionDtoAdapter =
    PolymorphicJsonAdapterFactory.of(TransactionDto::class.java, TransactionDto::txType::name.get())
        .withSubtype(MultisigTransactionDto::class.java, TransactionType.MULTISIG_TRANSACTION.name)
        .withSubtype(EthereumTransactionDto::class.java, TransactionType.ETHEREUM_TRANSACTION.name)
        .withSubtype(ModuleTransactionDto::class.java, TransactionType.MODULE_TRANSACTION.name)
        .withDefaultValue(UnknownTransactionDto)

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
        .add(WeiAdapter())
        .add(DecimalNumberAdapter())
        .add(BigDecimalNumberAdapter())
        .add(HexNumberAdapter())
        .add(DefaultNumberAdapter())
        .add(SolidityAddressAdapter())
        .add(OperationEnumAdapter())
        .add(transferInfoAdapter)
        .add(transactionInfoAdapter)
//        .add(transactionDtoAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()
