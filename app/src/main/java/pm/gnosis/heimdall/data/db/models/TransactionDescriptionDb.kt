package pm.gnosis.heimdall.data.db.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import pm.gnosis.models.Wei
import java.math.BigInteger


@Entity(tableName = TransactionDescriptionDb.TABLE_NAME)
data class TransactionDescriptionDb(
        @PrimaryKey
        @ColumnInfo(name = COL_ID)
        var id: String,

        @ColumnInfo(name = COL_SAFE_ADDRESS)
        var safeAddress: BigInteger,

        @ColumnInfo(name = COL_TO)
        var to: BigInteger,

        @ColumnInfo(name = COL_VALUE)
        var value: BigInteger,

        @ColumnInfo(name = COL_DATA)
        var data: String,

        @ColumnInfo(name = COL_OPERATION)
        var operation: BigInteger,

        @ColumnInfo(name = COL_NONCE)
        var nonce: BigInteger,

        @ColumnInfo(name = COL_SUBMITTED_AT)
        var submittedAt: Long,

        @ColumnInfo(name = COL_SUBJECT)
        var subject: String?,

        @ColumnInfo(name = COL_TX_HASH)
        var transactionHash: String
) {
    companion object {
        const val TABLE_NAME = "transaction_details"
        const val COL_ID = "id"
        const val COL_SAFE_ADDRESS = "safeAddress"
        const val COL_TO = "to"
        const val COL_VALUE = "value"
        const val COL_DATA = "data"
        const val COL_OPERATION = "operation"
        const val COL_NONCE = "nonce"
        const val COL_SUBMITTED_AT = "submittedAt"
        const val COL_SUBJECT = "subject"
        const val COL_TX_HASH = "txHash"
    }
}

fun GnosisSafeTransactionDescription.toDb(descriptionHash: String) =
        TransactionDescriptionDb(descriptionHash, safeAddress, to, value.value, data, operation, nonce, submittedAt, subject, transactionHash)

fun TransactionDescriptionDb.fromDb() =
        GnosisSafeTransactionDescription(safeAddress, to, Wei(value), data, operation, nonce, submittedAt, subject, transactionHash)
