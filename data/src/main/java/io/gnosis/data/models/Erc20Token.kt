package io.gnosis.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pm.gnosis.model.Solidity

@Entity(tableName = Erc20Token.TABLE_NAME)
data class Erc20Token(

    @PrimaryKey
    @ColumnInfo(name = COL_ADDRESS)
    var address: Solidity.Address,

    @ColumnInfo(name = COL_NAME)
    var name: String,

    @ColumnInfo(name = COL_SYMBOL)
    var symbol: String,

    @ColumnInfo(name = COL_DECIMALS)
    var decimals: Int,

    @ColumnInfo(name = COL_LOGO_URL)
    var logoUrl: String
) {
    companion object {
        const val TABLE_NAME = "erc20_tokens"
        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
        const val COL_SYMBOL = "symbol"
        const val COL_DECIMALS = "decimals"
        const val COL_LOGO_URL = "logoUrl"
    }
}
