package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.*
import io.reactivex.Flowable

@Dao
interface ERC20TokenDao {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertERC20Token(erC20Token: ERC20TokenDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertERC20Tokens(erC20Tokens: List<ERC20TokenDb>)

    @Query("SELECT * FROM ${ERC20TokenDb.TABLE_NAME} ORDER BY ${ERC20TokenDb.NAME_COL} ASC")
    fun observeTokens(): Flowable<List<ERC20TokenDb>>

    @Delete
    fun deleteToken(erC20Token: ERC20TokenDb)
}
