package pm.gnosis.heimdall.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.models.ERC20TokenDb
import pm.gnosis.model.Solidity

@Dao
interface ERC20TokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertERC20Token(erC20Token: ERC20TokenDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertERC20Tokens(erC20Tokens: List<ERC20TokenDb>)

    @Query("SELECT * FROM ${ERC20TokenDb.TABLE_NAME}")
    fun observeTokens(): Flowable<List<ERC20TokenDb>>

    @Query("SELECT * FROM ${ERC20TokenDb.TABLE_NAME}")
    fun loadTokens(): Single<List<ERC20TokenDb>>

    @Query("SELECT * FROM ${ERC20TokenDb.TABLE_NAME} WHERE ${ERC20TokenDb.COL_ADDRESS} = :address")
    fun observeToken(address: Solidity.Address): Flowable<ERC20TokenDb>

    @Query("SELECT * FROM ${ERC20TokenDb.TABLE_NAME} WHERE ${ERC20TokenDb.COL_ADDRESS} = :address")
    fun loadToken(address: Solidity.Address): Single<ERC20TokenDb>

    @Query("DELETE FROM ${ERC20TokenDb.TABLE_NAME} WHERE ${ERC20TokenDb.COL_ADDRESS} = :address")
    fun deleteToken(address: Solidity.Address)
}
