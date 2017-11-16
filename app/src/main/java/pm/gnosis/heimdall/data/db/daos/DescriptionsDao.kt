package pm.gnosis.heimdall.data.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb

@Dao
interface DescriptionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDescription(description: TransactionDescriptionDb)

    @Query("DELETE FROM ${TransactionDescriptionDb.TABLE_NAME} WHERE ${TransactionDescriptionDb.COL_HASH} = :hash")
    fun removeDescriptions(hash: String)

    @Query("SELECT * FROM ${TransactionDescriptionDb.TABLE_NAME} WHERE ${TransactionDescriptionDb.COL_HASH} = :hash")
    fun loadDescription(hash: String): Single<TransactionDescriptionDb>
}
