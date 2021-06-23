package io.gnosis.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.gnosis.data.models.Chain.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
data class Chain(
    @PrimaryKey
    @ColumnInfo(name = COL_CHAIN_ID)
    val chainId: Int,

    @ColumnInfo(name = COL_NAME)
    val name: String

) {
    companion object {
        const val TABLE_NAME = "chains"

        const val COL_NAME = "chain_name"
        const val COL_CHAIN_ID = "chain_id"
    }
}
