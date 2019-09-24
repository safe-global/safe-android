package pm.gnosis.heimdall.data.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.toAuthenticatorType
import pm.gnosis.model.Solidity

@Entity(tableName = AuthenticatorInfoDb.TABLE_NAME)
data class AuthenticatorInfoDb(
    @PrimaryKey
    @ColumnInfo(name = COL_ADDRESS)
    var address: Solidity.Address,

    @ColumnInfo(name = COL_TYPE)
    var type: Int,

    @ColumnInfo(name = COL_KEY_INDEX)
    var keyIndex: Long?
) {
    companion object {
        const val TABLE_NAME = "authenticator_info"
        const val COL_ADDRESS = "address"
        const val COL_TYPE = "type"
        const val COL_KEY_INDEX = "key_index"
    }
}

fun AuthenticatorInfoDb.fromDb() = AuthenticatorInfo(type.toAuthenticatorType()!!, address, keyIndex)
fun AuthenticatorInfo.toDb() = AuthenticatorInfoDb(address, type.id, keyIndex)
