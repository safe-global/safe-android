package pm.gnosis.android.app.accounts.data.db

import android.arch.persistence.room.*
import io.reactivex.Single
import pm.gnosis.android.app.accounts.repositories.impl.models.db.AccountDb

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertAccount(account: AccountDb): Long

    @Query("SELECT * FROM ${AccountDb.TABLE_NAME} WHERE ${AccountDb.PRIVATE_KEY_COL} = :privateKey")
    fun observeAccount(privateKey: String): Single<AccountDb>

    @Query("SELECT * FROM ${AccountDb.TABLE_NAME}")
    fun observeAccounts(): Single<AccountDb>

    @Delete
    fun deleteAccount(account: AccountDb)
}
