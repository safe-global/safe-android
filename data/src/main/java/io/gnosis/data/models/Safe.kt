package io.gnosis.data.models

import androidx.room.*
import io.gnosis.data.BuildConfig
import io.gnosis.data.models.Safe.Companion.TABLE_NAME
import pm.gnosis.model.Solidity

@Entity(tableName = TABLE_NAME)
data class Safe(
    @PrimaryKey
    @ColumnInfo(name = COL_ADDRESS)
    val address: Solidity.Address,

    @ColumnInfo(name = COL_LOCAL_NAME)
    val localName: String,

    @ColumnInfo(name = COL_CHAIN_ID)
    val chainId: Int = BuildConfig.CHAIN_ID
) {

    @Ignore
    var chain: Chain? = null

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
    val chain: Chain?
)
