package pm.gnosis.heimdall.data.db.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey


@Entity(tableName = TransactionPublishStatusDb.TABLE_NAME)
data class TransactionPublishStatusDb(
        @PrimaryKey
        @ColumnInfo(name = COL_ID)
        var id: String,

        @ColumnInfo(name = COL_TRANSACTION_ID)
        var transactionId: String,

        @ColumnInfo(name = COL_SUCCESS)
        var success: Boolean?
) {
    companion object {
        const val TABLE_NAME = "transaction_publish_status"
        const val COL_ID = "id"
        const val COL_TRANSACTION_ID = "transactionId"
        const val COL_SUCCESS = "success"
    }
}
