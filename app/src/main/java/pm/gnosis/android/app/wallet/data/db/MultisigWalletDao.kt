package pm.gnosis.android.app.wallet.data.db

import android.arch.persistence.room.*
import io.reactivex.Flowable

@Dao
interface MultisigWalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMultisigWallet(multisigWallet: MultisigWallet)

    @Query("SELECT * FROM ${MultisigWallet.TABLE_NAME}")
    fun observeMultisigWallets(): Flowable<List<MultisigWallet>>

    @Delete
    fun removeMultisigWallet(multisigWallet: MultisigWallet)

    @Update
    fun updateMultisigWallet(multisigWallet: MultisigWallet)
}
