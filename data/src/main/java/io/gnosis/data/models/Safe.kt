package io.gnosis.data.models

import androidx.room.*
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
    val chainId: BigInteger = BuildConfig.CHAIN_ID.toBigInteger(),

    @ColumnInfo(name = COL_VERSION)
    val version: String? = null
) {

    @Ignore
    var chain: Chain = Chain.DEFAULT_CHAIN

    @Ignore
    var signingOwners: List<Solidity.Address> = listOf()

    val readOnly: Boolean
        get() = signingOwners.isEmpty()

    companion object {
        const val TABLE_NAME = "safes"

        const val COL_ADDRESS = "address"
        const val COL_LOCAL_NAME = "local_name"
        const val COL_CHAIN_ID = "chain_id"
        const val COL_VERSION = "version"
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
