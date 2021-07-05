package io.gnosis.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import io.gnosis.data.BuildConfig
import io.gnosis.data.models.SafeMetaData.Companion.COL_ADDRESS
import io.gnosis.data.models.SafeMetaData.Companion.COL_CHAIN_ID
import pm.gnosis.model.Solidity

@Entity(
    tableName = SafeMetaData.TABLE_NAME,
    primaryKeys = [COL_ADDRESS, COL_CHAIN_ID],
    foreignKeys = [

        ForeignKey(
            entity = Safe::class,
            parentColumns = [Safe.COL_ADDRESS, Safe.COL_CHAIN_ID],
            childColumns = [COL_ADDRESS, COL_CHAIN_ID],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
data class SafeMetaData(
    @ColumnInfo(name = COL_ADDRESS)
    val address: Solidity.Address,

    @ColumnInfo(name = COL_CHAIN_ID)
    val chainId: Int = BuildConfig.CHAIN_ID,

    @ColumnInfo(name = COL_REGISTERED_NOTIFICATIONS)
    val registeredNotifications: Boolean
) {
    companion object {
        const val TABLE_NAME = "safe_meta_data"

        const val COL_ADDRESS = "address"
        const val COL_CHAIN_ID = "chain_id"
        const val COL_REGISTERED_NOTIFICATIONS = "registered_notifications"
    }
}
