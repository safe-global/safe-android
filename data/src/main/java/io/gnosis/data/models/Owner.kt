package io.gnosis.data.models

import androidx.room.*
import io.gnosis.data.models.Owner.Companion.TABLE_NAME
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.svalinn.security.db.EncryptedString
import pm.gnosis.utils.utf8String

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
    @TypeConverters(EncryptedByteArray.NullableConverter::class)
    val privateKey: EncryptedByteArray? = null,

    @ColumnInfo(name = COL_SEED_PHRASE)
    @TypeConverters(EncryptedString.NullableConverter::class)
    val seedPhrase: EncryptedString? = null
) {

    enum class Type(val value: Int) {
        // add types here
        IMPORTED(0),
        GENERATED(1);

        companion object {
            fun get(value: Int) = when (value) {
                0 -> IMPORTED
                1 -> GENERATED
                else -> IMPORTED
            }
        }
    }

    companion object {
        const val TABLE_NAME = "owners"

        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
        const val COL_TYPE = "type"
        const val COL_PRIVATE_KEY = "private_key"
        const val COL_SEED_PHRASE = "seed_phrase"
    }
}

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
