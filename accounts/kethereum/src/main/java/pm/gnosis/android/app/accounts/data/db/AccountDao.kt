package pm.gnosis.android.app.accounts.data.db

import android.arch.persistence.room.*
import io.reactivex.Single

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertAccount(account: Account)

    @Query("SELECT * FROM ${Account.TABLE_NAME} WHERE ${Account.PRIVATE_KEY_COL} = :privateKey")
    fun observeAccount(privateKey: String): Single<Account>

    @Query("SELECT * FROM ${Account.TABLE_NAME}")
    fun observeAccounts(): Single<Account>

    @Delete
    fun deleteAccount(account: Account)
}
