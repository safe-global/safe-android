package io.gnosis.data.models

import androidx.room.*
import io.gnosis.data.models.Owner.Companion.TABLE_NAME
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.svalinn.security.db.EncryptedString

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
    val seedPhrase: EncryptedString? = null,

    @ColumnInfo(name = COL_KEY_DERIVATION_PATH)
    val keyDerivationPath: String? = null

    //FIXME: add device uuid?
) {

    enum class Type(val value: Int) {
        // add types here
        IMPORTED(0),
        GENERATED(1),
        LEDGER_NANO_X(2);

        companion object {
            fun get(value: Int) = when (value) {
                0 -> IMPORTED
                1 -> GENERATED
                2 -> LEDGER_NANO_X
                else -> IMPORTED
            }
        }

        override fun toString(): String =
            when(value) {
                0 -> "Imported"
                1 -> "Generated"
                2 -> "Ledger Nano X"
                else -> "Imported"
            }
    }

    companion object {
        const val TABLE_NAME = "owners"

        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
        const val COL_TYPE = "type"
        const val COL_PRIVATE_KEY = "private_key"
        const val COL_SEED_PHRASE = "seed_phrase"
        const val COL_KEY_DERIVATION_PATH = "derivation_path"
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
