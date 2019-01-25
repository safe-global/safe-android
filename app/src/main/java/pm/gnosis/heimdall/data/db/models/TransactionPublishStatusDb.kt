package pm.gnosis.heimdall.data.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = TransactionPublishStatusDb.TABLE_NAME)
data class TransactionPublishStatusDb(
    @PrimaryKey
    @ColumnInfo(name = COL_ID)
    var id: String,

    @ColumnInfo(name = COL_TRANSACTION_ID)
    var transactionId: String,

    @ColumnInfo(name = COL_SUCCESS)
    var success: Boolean?,

    @ColumnInfo(name = COL_TIMESTAMP)
    var timestamp: Long?
) {
    companion object {
        const val TABLE_NAME = "transaction_publish_status"
        const val COL_ID = "id"
        const val COL_TRANSACTION_ID = "transactionId"
        const val COL_SUCCESS = "success"
        const val COL_TIMESTAMP = "timestamp"
    }
}
