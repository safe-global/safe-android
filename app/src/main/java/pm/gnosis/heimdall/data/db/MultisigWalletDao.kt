package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.*
import io.reactivex.Flowable
import pm.gnosis.heimdall.data.db.model.MultisigWalletDb

@Dao
interface MultisigWalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMultisigWallet(multisigWallet: MultisigWalletDb)

    @Query("SELECT * FROM ${MultisigWalletDb.TABLE_NAME}")
    fun observeMultisigWallets(): Flowable<List<MultisigWalletDb>>

    @Query("SELECT * FROM ${MultisigWalletDb.TABLE_NAME} WHERE ${MultisigWalletDb.COL_ADDRESS} = :address")
    fun observeMultisigWallet(address: String): Flowable<MultisigWalletDb>

    @Query("DELETE FROM ${MultisigWalletDb.TABLE_NAME} WHERE ${MultisigWalletDb.COL_ADDRESS} = :address")
    fun removeMultisigWallet(address: String)

    @Update
    fun updateMultisigWallet(multisigWallet: MultisigWalletDb)
}
