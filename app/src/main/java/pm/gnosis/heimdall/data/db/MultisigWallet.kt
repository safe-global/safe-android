package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull

@Entity(tableName = MultisigWallet.TABLE_NAME)
class MultisigWallet {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = COL_ADDRESS)
    var address: String? = null
    @ColumnInfo(name = COL_NAME)
    var name: String? = null

    companion object {
        const val TABLE_NAME = "multisig_wallets"
        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
    }
}
