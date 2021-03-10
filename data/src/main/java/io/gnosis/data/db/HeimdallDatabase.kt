package io.gnosis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Owner
import io.gnosis.data.models.OwnerTypeConverter
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeMetaData
import pm.gnosis.svalinn.security.db.EncryptedByteArray

@Database(
    entities = [
        Safe::class,
        SafeMetaData::class,
        Owner::class
    ], version = HeimdallDatabase.LATEST_DB_VERSION
)
@TypeConverters(SolidityAddressConverter::class, OwnerTypeConverter::class, EncryptedByteArray.Converter::class)
abstract class HeimdallDatabase : RoomDatabase() {

    abstract fun safeDao(): SafeDao

    companion object {
        const val DB_NAME = "safe_db"
        const val LATEST_DB_VERSION = 3

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """DROP TABLE IF EXISTS `erc20_tokens`"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //TODO: add migration
//                database.execSQL(
//
//                )
            }
        }
    }
}
