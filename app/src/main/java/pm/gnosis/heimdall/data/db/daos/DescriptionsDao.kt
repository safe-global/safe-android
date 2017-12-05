package pm.gnosis.heimdall.data.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import java.math.BigInteger

@Dao
interface DescriptionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDescription(description: TransactionDescriptionDb)

    @Query("DELETE FROM ${TransactionDescriptionDb.TABLE_NAME} WHERE ${TransactionDescriptionDb.COL_ID} = :id")
    fun removeDescriptions(id: String)

    @Query("SELECT * FROM ${TransactionDescriptionDb.TABLE_NAME} WHERE ${TransactionDescriptionDb.COL_ID} = :id")
    fun loadDescription(id: String): Single<TransactionDescriptionDb>

    @Query("SELECT ${TransactionDescriptionDb.COL_ID} FROM ${TransactionDescriptionDb.TABLE_NAME} WHERE ${TransactionDescriptionDb.COL_SAFE_ADDRESS} = :safe ORDER BY ${TransactionDescriptionDb.COL_SUBMITTED_AT} DESC")
    fun observeDescriptions(safe: BigInteger): Flowable<List<String>>
}
