package pm.gnosis.heimdall.data.db.daos

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import pm.gnosis.heimdall.data.db.models.TransactionPublishStatusDb
import pm.gnosis.model.Solidity

@Dao
interface DescriptionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(description: TransactionDescriptionDb)

    @Query("SELECT * FROM ${TransactionDescriptionDb.TABLE_NAME} WHERE ${TransactionDescriptionDb.COL_ID} = :id")
    fun loadDescription(id: String): Single<TransactionDescriptionDb>

    @Query("SELECT ${TransactionDescriptionDb.COL_ID} FROM ${TransactionDescriptionDb.TABLE_NAME} WHERE ${TransactionDescriptionDb.COL_SAFE_ADDRESS} = :safe ORDER BY ${TransactionDescriptionDb.COL_SUBMITTED_AT} DESC")
    fun observeDescriptions(safe: Solidity.Address): Flowable<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(status: TransactionPublishStatusDb)

    @Update
    fun update(status: TransactionPublishStatusDb)

    @Query("SELECT * FROM ${TransactionPublishStatusDb.TABLE_NAME} WHERE ${TransactionPublishStatusDb.COL_ID} = :id")
    fun observeStatus(id: String): Flowable<TransactionPublishStatusDb>
}
