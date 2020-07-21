package io.gnosis.data.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.backend.dto.*
import io.gnosis.data.db.BigDecimalNumberAdapter
import pm.gnosis.common.adapters.moshi.*

internal val transactionDtoAdapter =
    PolymorphicJsonAdapterFactory.of(TransactionDto::class.java, TransactionDto::txType::name.get())
        .withSubtype(MultisigTransactionDto::class.java, TransactionType.MULTISIG_TRANSACTION.name)
        .withSubtype(EthereumTransactionDto::class.java, TransactionType.ETHEREUM_TRANSACTION.name)
        .withSubtype(ModuleTransactionDto::class.java, TransactionType.MODULE_TRANSACTION.name)
        .withDefaultValue(UnknownTransactionDto)

val dataMoshi =
    Moshi.Builder()
        .add(WeiAdapter())
        .add(DecimalNumberAdapter())
        .add(BigDecimalNumberAdapter())
        .add(HexNumberAdapter())
        .add(DefaultNumberAdapter())
        .add(SolidityAddressAdapter())
        .add(OperationEnumAdapter())
        .add(transactionDtoAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()
