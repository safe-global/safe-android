package io.gnosis.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.gnosis.data.models.Chain.Companion.TABLE_NAME
import java.io.Serializable

@Entity(tableName = TABLE_NAME)
data class Chain(
    @PrimaryKey
    @ColumnInfo(name = COL_CHAIN_ID)
    val chainId: Int,

    @ColumnInfo(name = COL_CHAIN_NAME)
    val name: String,

    @ColumnInfo(name = COL_TEXT_COLOR)
    val textColor: String,

    @ColumnInfo(name = COL_BACKGROUND_COLOR)
    val backgroundColor: String,

//    @ColumnInfo(name = COL_ENS_REGISTRY_ADDRESS)
    val ensRegistryAddress: String?

) : Serializable {
    companion object {
        const val TABLE_NAME = "chains"

        const val COL_CHAIN_NAME = "chain_name"
        const val COL_CHAIN_ID = "chain_id"
        const val COL_BACKGROUND_COLOR = "background_color"
//        const val COL_ENS_REGISTRY_ADDRESS = "ens_registry_address"
        const val COL_TEXT_COLOR = "text_color"
    }
}
