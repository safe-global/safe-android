package pm.gnosis.heimdall.data.db.daos

import androidx.room.*
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

    @Query("SELECT ${TransactionDescriptionDb.TABLE_NAME}.${TransactionDescriptionDb.COL_ID}, ${TransactionDescriptionDb.COL_SUBMITTED_AT} as timestamp FROM ${TransactionDescriptionDb.TABLE_NAME} LEFT JOIN ${TransactionPublishStatusDb.TABLE_NAME} ON ${TransactionDescriptionDb.TABLE_NAME}.${TransactionDescriptionDb.COL_ID} = ${TransactionPublishStatusDb.TABLE_NAME}.${TransactionPublishStatusDb.COL_ID} WHERE ${TransactionDescriptionDb.COL_SAFE_ADDRESS} = :safe AND ${TransactionPublishStatusDb.COL_TIMESTAMP} IS NULL ORDER BY ${TransactionDescriptionDb.COL_SUBMITTED_AT} DESC")
    fun observePendingTransaction(safe: Solidity.Address): Flowable<List<TransactionWithTimestamp>>

    @Query("SELECT ${TransactionDescriptionDb.TABLE_NAME}.${TransactionDescriptionDb.COL_ID}, ${TransactionDescriptionDb.COL_SUBMITTED_AT} as timestamp FROM ${TransactionDescriptionDb.TABLE_NAME} LEFT JOIN ${TransactionPublishStatusDb.TABLE_NAME} ON ${TransactionDescriptionDb.TABLE_NAME}.${TransactionDescriptionDb.COL_ID} = ${TransactionPublishStatusDb.TABLE_NAME}.${TransactionPublishStatusDb.COL_ID} WHERE ${TransactionDescriptionDb.COL_SAFE_ADDRESS} = :safe AND ${TransactionPublishStatusDb.COL_TIMESTAMP} IS NOT NULL ORDER BY ${TransactionPublishStatusDb.COL_TIMESTAMP} DESC, ${TransactionDescriptionDb.COL_SUBMITTED_AT} DESC")
    fun observeSubmittedTransaction(safe: Solidity.Address): Flowable<List<TransactionWithTimestamp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(status: TransactionPublishStatusDb)

    @Update
    fun update(status: TransactionPublishStatusDb)

    @Query("SELECT * FROM ${TransactionPublishStatusDb.TABLE_NAME} WHERE ${TransactionPublishStatusDb.COL_ID} = :id")
    fun loadStatus(id: String): Single<TransactionPublishStatusDb>

    @Query("SELECT * FROM ${TransactionPublishStatusDb.TABLE_NAME} WHERE ${TransactionPublishStatusDb.COL_ID} = :id")
    fun observeStatus(id: String): Flowable<TransactionPublishStatusDb>

    @Transaction
    fun insert(transactionDescriptionDb: TransactionDescriptionDb, status: TransactionPublishStatusDb) {
        insert(transactionDescriptionDb)
        insert(status)
    }

    data class TransactionWithTimestamp(val id: String, val timestamp: Long)
}
