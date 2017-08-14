package pm.gnosis.android.app.wallet.data.db

import android.arch.persistence.room.*
import io.reactivex.Flowable

@Dao
interface ERC20TokenDao {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertERC20Token(erC20Token: ERC20Token)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertERC20Tokens(erC20Tokens: List<ERC20Token>)

    @Query("SELECT * FROM ${ERC20Token.TABLE_NAME} ORDER BY ${ERC20Token.NAME_COL} ASC")
    fun observeTokens(): Flowable<List<ERC20Token>>

    @Delete
    fun deleteToken(erC20Token: ERC20Token)
}
