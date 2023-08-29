package io.gnosis.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import io.gnosis.data.models.TransactionLocal.Companion.COL_CHAIN_ID
import io.gnosis.data.models.TransactionLocal.Companion.COL_SAFE_ADDRESS
import io.gnosis.data.models.TransactionLocal.Companion.COL_SAFE_TX_HASH
import io.gnosis.data.models.TransactionLocal.Companion.TABLE_NAME
import io.gnosis.data.models.transaction.TransactionStatus
import pm.gnosis.model.Solidity
import java.math.BigInteger

@Entity(
    tableName = TABLE_NAME,
    primaryKeys = [COL_SAFE_ADDRESS, COL_CHAIN_ID, COL_SAFE_TX_HASH]
)
data class TransactionLocal(

    @ColumnInfo(name = COL_CHAIN_ID)
    val chainId: BigInteger,

    @ColumnInfo(name = COL_SAFE_ADDRESS)
    val safeAddress: Solidity.Address,

    @ColumnInfo(name = COL_SAFE_TX_NONCE)
    val safeTxNonce: BigInteger,

    @ColumnInfo(name = COL_SAFE_TX_HASH)
    val safeTxHash: String,

    @ColumnInfo(name = COL_ETH_TX_HASH)
    val ethTxHash: String,

    @ColumnInfo(name = COL_STATUS)
    val status: TransactionStatus,

    @ColumnInfo(name = COL_SUBMITTED_AT)
    val submittedAt: Long,
) {
    companion object {
        const val TABLE_NAME = "local_transactions"

        const val COL_CHAIN_ID = "chain_id"
        const val COL_SAFE_ADDRESS = "safe_address"
        const val COL_SAFE_TX_NONCE = "safe_tx_nonce"
        const val COL_SAFE_TX_HASH = "safe_tx_hash"
        const val COL_ETH_TX_HASH = "eth_tx_hash"
        const val COL_STATUS = "status"
        const val COL_SUBMITTED_AT = "submitted_at"
    }
}
