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
        Chain::class,
        Chain.Currency::class
    ], version = HeimdallDatabase.LATEST_DB_VERSION
)
@TypeConverters(
    BigIntegerConverter::class,
    SolidityAddressConverter::class,
    OwnerTypeConverter::class,
    EncryptedByteArray.NullableConverter::class,
    EncryptedString.NullableConverter::class,
    RpcAuthenticationConverter::class
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
                val defaultChainId = BigIntegerConverter().toHexString(BuildConfig.CHAIN_ID.toBigInteger())
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${Chain.TABLE_NAME}` (`${Chain.COL_CHAIN_ID}` TEXT NOT NULL, `${Chain.COL_CHAIN_NAME}` TEXT NOT NULL, `${Chain.COL_TEXT_COLOR}` TEXT NOT NULL, `${Chain.COL_BACKGROUND_COLOR}` TEXT NOT NULL, `${Chain.COL_RPC_URI}` TEXT NOT NULL, `${Chain.COL_RPC_AUTHENTICATION}` INTEGER NOT NULL, `${Chain.COL_BLOCK_EXPLORER_URI}` TEXT NOT NULL, `${Chain.COL_ENS_REGISTRY_ADDRESS}` TEXT, PRIMARY KEY(`${Chain.COL_CHAIN_ID}`))"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${Chain.Currency.TABLE_NAME}` (`${Chain.Currency.COL_CHAIN_ID}` TEXT NOT NULL, `${Chain.Currency.COL_NAME}` TEXT NOT NULL, `${Chain.Currency.COL_SYMBOL}` TEXT NOT NULL, `${Chain.Currency.COL_DECIMALS}` INTEGER NOT NULL, `${Chain.Currency.COL_LOGO_URI}` TEXT NOT NULL, PRIMARY KEY(`${Chain.Currency.COL_CHAIN_ID}`), FOREIGN KEY(`${Chain.Currency.COL_CHAIN_ID}`) REFERENCES `${Chain.TABLE_NAME}`(`${Chain.COL_CHAIN_ID}`) ON UPDATE CASCADE ON DELETE CASCADE )"""
                )

                // Add CHAIN_ID column and add it to primary key
                database.execSQL(
                    """ALTER TABLE `${SafeMetaData.TABLE_NAME}` ADD COLUMN `${SafeMetaData.COL_CHAIN_ID}` TEXT NOT NULL DEFAULT `${defaultChainId}`"""
                )
                database.execSQL(
                    """ALTER TABLE `${SafeMetaData.TABLE_NAME}` RENAME TO `${SafeMetaData.TABLE_NAME}_old`"""
                )
                database.execSQL(
                    """CREATE TABLE `${SafeMetaData.TABLE_NAME}` (`address` TEXT NOT NULL, `chain_id` TEXT NOT NULL DEFAULT `${defaultChainId}`, `registered_notifications` INTEGER NOT NULL, PRIMARY KEY(`address`, `chain_id`), FOREIGN KEY(`address`, `chain_id`) REFERENCES `safes`(`address`, `chain_id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)"""
                )
                database.execSQL(
                    """INSERT INTO ${SafeMetaData.TABLE_NAME} SELECT * FROM ${SafeMetaData.TABLE_NAME}_old"""
                )
                database.execSQL(
                    """DROP TABLE IF EXISTS `${SafeMetaData.TABLE_NAME}_old`"""
                )

                // Add CHAIN_ID column and add it to primary key
                database.execSQL(
                    """ALTER TABLE `${Safe.TABLE_NAME}` ADD COLUMN `${Safe.COL_CHAIN_ID}` TEXT NOT NULL DEFAULT `${defaultChainId}`"""
                )
                database.execSQL(
                    """ALTER TABLE `${Safe.TABLE_NAME}` RENAME TO `${Safe.TABLE_NAME}_old`"""
                )
                database.execSQL(
                    """CREATE TABLE `${Safe.TABLE_NAME}` (`address` TEXT NOT NULL, `local_name` TEXT NOT NULL, `chain_id` TEXT NOT NULL DEFAULT `${defaultChainId}`, PRIMARY KEY(`address`, `chain_id`))"""
                )
                database.execSQL(
                    """INSERT INTO ${Safe.TABLE_NAME} SELECT * FROM ${Safe.TABLE_NAME}_old"""
                )
                database.execSQL(
                    """DROP TABLE IF EXISTS `${Safe.TABLE_NAME}_old`"""
                )
            }
        }
    }
}
