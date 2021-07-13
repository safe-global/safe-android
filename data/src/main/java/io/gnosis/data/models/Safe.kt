package io.gnosis.data.models

import androidx.room.*
import io.gnosis.contracts.BuildConfig.ENS_REGISTRY
import io.gnosis.data.BuildConfig
import io.gnosis.data.models.Safe.Companion.COL_ADDRESS
import io.gnosis.data.models.Safe.Companion.COL_CHAIN_ID
import io.gnosis.data.models.Safe.Companion.TABLE_NAME
import pm.gnosis.model.Solidity
import java.math.BigInteger

@Entity(
    tableName = TABLE_NAME,
    primaryKeys = [COL_ADDRESS, COL_CHAIN_ID]
)
data class Safe(
    @ColumnInfo(name = COL_ADDRESS)
    val address: Solidity.Address,

    @ColumnInfo(name = COL_LOCAL_NAME)
    val localName: String,

    @ColumnInfo(name = COL_CHAIN_ID)
    val chainId: BigInteger = BuildConfig.CHAIN_ID.toBigInteger()
) {

    @Ignore
    var chain: Chain = Chain(
        BuildConfig.CHAIN_ID.toBigInteger(),
        BuildConfig.BLOCKCHAIN_NAME,
        BuildConfig.CHAIN_TEXT_COLOR,
        BuildConfig.CHAIN_BACKGROUND_COLOR,
        ENS_REGISTRY
    )

    companion object {
        const val TABLE_NAME = "safes"

        const val COL_ADDRESS = "address"
        const val COL_LOCAL_NAME = "local_name"
        const val COL_CHAIN_ID = "chain_id"
    }
}

data class SafeWithChainData(
    @Embedded
    val safe: Safe,
    @Relation(parentColumn = Safe.COL_CHAIN_ID, entityColumn = Chain.COL_CHAIN_ID)
    val chain: Chain?,
    @Relation(parentColumn = Safe.COL_CHAIN_ID, entityColumn = Chain.Currency.COL_CHAIN_ID)
    val currency: Chain.Currency?
)
