package io.gnosis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.gnosis.data.BuildConfig
import io.gnosis.data.db.daos.ChainDao
import io.gnosis.data.db.daos.OwnerDao
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.*
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.svalinn.security.db.EncryptedString

@Database(
    entities = [
        Safe::class,
        SafeMetaData::class,
        Owner::class,
        Chain::class
    ], version = HeimdallDatabase.LATEST_DB_VERSION
)
@TypeConverters(
    SolidityAddressConverter::class,
    OwnerTypeConverter::class,
    EncryptedByteArray.NullableConverter::class,
    EncryptedString.NullableConverter::class
)
abstract class HeimdallDatabase : RoomDatabase() {

    abstract fun safeDao(): SafeDao

    abstract fun ownerDao(): OwnerDao

    abstract fun chainDao(): ChainDao

    companion object {
        const val DB_NAME = "safe_db"
        const val LATEST_DB_VERSION = 5

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """DROP TABLE IF EXISTS `erc20_tokens`"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${Owner.TABLE_NAME}` (`${Owner.COL_ADDRESS}` TEXT NOT NULL, `${Owner.COL_NAME}` TEXT, `type` INTEGER NOT NULL, `private_key` TEXT, PRIMARY KEY(`${Owner.COL_ADDRESS}`))"""
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """ALTER TABLE `${Owner.TABLE_NAME}` ADD COLUMN `${Owner.COL_SEED_PHRASE}` TEXT"""
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${Chain.TABLE_NAME}` (`${Chain.COL_CHAIN_NAME}` TEXT NOT NULL, `${Chain.COL_TEXT_COLOR}` TEXT NOT NULL, `${Chain.COL_BACKGROUND_COLOR}` TEXT NOT NULL, `${Chain.COL_CHAIN_ID}` INTEGER NOT NULL, PRIMARY KEY(`${Chain.COL_CHAIN_ID}`))"""
                )
                database.execSQL(
                    """ALTER TABLE `${Safe.TABLE_NAME}` ADD COLUMN `${Safe.COL_CHAIN_ID}` INTEGER NOT NULL DEFAULT `${BuildConfig.CHAIN_ID}`"""
                )
            }
        }
    }
}
