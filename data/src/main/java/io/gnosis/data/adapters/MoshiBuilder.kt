package io.gnosis.data.adapters

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.backend.dto.*
import io.gnosis.data.db.BigDecimalNumberAdapter
import io.gnosis.data.models.*
import pm.gnosis.common.adapters.moshi.MoshiBuilderFactory

internal val transactionDtoAdapter =
    PolymorphicJsonAdapterFactory.of(TransactionDto::class.java, TransactionDto::txType::name.get())
        .withSubtype(MultisigTransactionDto::class.java, TransactionType.MULTISIG_TRANSACTION.name)
        .withSubtype(EthereumTransactionDto::class.java, TransactionType.ETHEREUM_TRANSACTION.name)
        .withSubtype(ModuleTransactionDto::class.java, TransactionType.MODULE_TRANSACTION.name)
        .withDefaultValue(UnknownTransactionDto)

val dataMoshi =
    MoshiBuilderFactory.makeMoshiBuilder()
        .add(BigDecimalNumberAdapter())
        .add(OperationEnumAdapter())
        .add(transactionDtoAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()
