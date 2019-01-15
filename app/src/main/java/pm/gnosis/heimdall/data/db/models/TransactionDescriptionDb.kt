package pm.gnosis.heimdall.data.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pm.gnosis.model.Solidity
import java.math.BigInteger


@Entity(tableName = TransactionDescriptionDb.TABLE_NAME)
data class TransactionDescriptionDb(
    @PrimaryKey
    @ColumnInfo(name = COL_ID)
    var id: String,

    @ColumnInfo(name = COL_SAFE_ADDRESS)
    var safeAddress: Solidity.Address,

    @ColumnInfo(name = COL_TO)
    var to: Solidity.Address,

    @ColumnInfo(name = COL_VALUE)
    var value: BigInteger,

    @ColumnInfo(name = COL_DATA)
    var data: String,

    @ColumnInfo(name = COL_OPERATION)
    var operation: BigInteger,

    @ColumnInfo(name = COL_TX_GAS)
    var txGas: BigInteger,

    @ColumnInfo(name = COL_DATA_GAS)
    var dataGas: BigInteger,

    @ColumnInfo(name = COL_GAS_TOKEN)
    var gasToken: Solidity.Address,

    @ColumnInfo(name = COL_GAS_PRICE)
    var gasPrice: BigInteger,

    @ColumnInfo(name = COL_NONCE)
    var nonce: BigInteger,

    @ColumnInfo(name = COL_SUBMITTED_AT)
    var submittedAt: Long,

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
        const val COL_TX_GAS = "txGas"
        const val COL_DATA_GAS = "dataGas"
        const val COL_GAS_TOKEN = "gasToken"
        const val COL_GAS_PRICE = "gasPrice"
        const val COL_NONCE = "nonce"
        const val COL_SUBMITTED_AT = "submittedAt"
        const val COL_TX_HASH = "txHash"
    }
}
