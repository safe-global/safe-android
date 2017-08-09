package pm.gnosis.android.app.wallet.data.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Flowable

@Dao
interface MultisigWalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMultisigWallet(multisigWallet: MultisigWallet)

    @Query("SELECT * FROM ${MultisigWallet.TABLE_NAME}")
    fun observeMultisigWallets(): Flowable<List<MultisigWallet>>
}
