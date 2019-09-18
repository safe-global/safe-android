package pm.gnosis.heimdall.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pm.gnosis.heimdall.data.db.models.AuthenticatorInfoDb
import pm.gnosis.heimdall.data.db.models.ERC20TokenDb
import pm.gnosis.model.Solidity

@Dao
interface AuthenticatorInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAuthenticatorInfo(info: AuthenticatorInfoDb)

    @Query("SELECT * FROM ${AuthenticatorInfoDb.TABLE_NAME} WHERE ${AuthenticatorInfoDb.COL_ADDRESS} = :address")
    fun loadAuthenticatorInfo(address: Solidity.Address): AuthenticatorInfoDb
}
