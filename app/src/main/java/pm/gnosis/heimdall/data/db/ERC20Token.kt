package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = ERC20Token.TABLE_NAME)
class ERC20Token {
    @PrimaryKey @ColumnInfo(name = ADDRESS_COL) var address: String? = null

    @ColumnInfo(name = NAME_COL) var name: String? = null
    @ColumnInfo(name = VERIFIED_COL) var verified = false

    companion object {
        const val TABLE_NAME = "erc20_tokens"
        const val ADDRESS_COL = "address"
        const val NAME_COL = "name"
        const val VERIFIED_COL = "verified"
    }
}
