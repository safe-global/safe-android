package io.gnosis.data.models

import androidx.room.*
import io.gnosis.data.models.Owner.Companion.TABLE_NAME
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.db.EncryptedByteArray

@ExcludeClassFromJacocoGeneratedReport
@Entity(
    tableName = TABLE_NAME
)
data class Owner(
    @PrimaryKey
    @ColumnInfo(name = COL_ADDRESS)
    val address: Solidity.Address,

    @ColumnInfo(name = COL_NAME)
    val name: String? = null,

    @ColumnInfo(name = COL_TYPE)
    @TypeConverters(OwnerTypeConverter::class)
    val type: Type,

    @ColumnInfo(name = COL_PRIVATE_KEY)
    val privateKey: EncryptedByteArray? = null
) {

    enum class Type(val value: Int) {
        // add types here
        LOCALLY_STORED(0);

        companion object {
            fun get(value: Int) = when (value) {
                0 -> LOCALLY_STORED
                else -> LOCALLY_STORED
            }
        }
    }

    companion object {
        const val TABLE_NAME = "owners"

        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
        const val COL_TYPE = "type"
        const val COL_PRIVATE_KEY = "private_key"
    }
}

@ExcludeClassFromJacocoGeneratedReport
class OwnerTypeConverter {

    @TypeConverter
    fun toType(typeValue: Int): Owner.Type {
        return Owner.Type.get(typeValue)
    }

    @TypeConverter
    fun toValue(type: Owner.Type): Int {
        return type.value
    }
}
