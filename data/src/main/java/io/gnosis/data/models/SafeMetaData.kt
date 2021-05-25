package io.gnosis.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import pm.gnosis.model.Solidity

@Entity(
    tableName = SafeMetaData.TABLE_NAME,
    foreignKeys = [

        ForeignKey(
            entity = Safe::class,
            parentColumns = [Safe.COL_ADDRESS],
            childColumns = [SafeMetaData.COL_ADDRESS],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
data class SafeMetaData(
    @PrimaryKey
    @ColumnInfo(name = COL_ADDRESS)
    val address: Solidity.Address,

    @ColumnInfo(name = COL_REGISTERED_NOTIFICATIONS)
    val registeredNotifications: Boolean
) {
    companion object {
        const val TABLE_NAME = "safe_meta_data"

        const val COL_ADDRESS = "address"
        const val COL_REGISTERED_NOTIFICATIONS = "registered_notifications"
    }
}
