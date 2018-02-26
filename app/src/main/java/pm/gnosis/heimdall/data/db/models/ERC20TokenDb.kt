package pm.gnosis.heimdall.data.db.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.math.BigInteger

@Entity(tableName = ERC20TokenDb.TABLE_NAME)
data class ERC20TokenDb(
    @PrimaryKey
    @ColumnInfo(name = COL_ADDRESS)
    var address: BigInteger,

    @ColumnInfo(name = COL_NAME)
    var name: String?,

    @ColumnInfo(name = COL_SYMBOL)
    var symbol: String?,

    @ColumnInfo(name = COL_DECIMALS)
    var decimals: Int,

    @ColumnInfo(name = COL_VERIFIED)
    var verified: Boolean
) {
    companion object {
        const val TABLE_NAME = "erc20_tokens"
        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
        const val COL_SYMBOL = "symbol"
        const val COL_DECIMALS = "decimals"
        const val COL_VERIFIED = "verified"
    }
}
