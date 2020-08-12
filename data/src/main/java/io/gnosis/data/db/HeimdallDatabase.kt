package io.gnosis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Erc20Token
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeMetaData

@Database(
    entities = [
        Safe::class,
        SafeMetaData::class,
        Erc20Token::class
    ], version = HeimdallDatabase.LATEST_DB_VERSION
)
@TypeConverters(SolidityAddressConverter::class)
abstract class HeimdallDatabase : RoomDatabase() {

    abstract fun safeDao(): SafeDao

    abstract fun erc20TokenDao(): Erc20TokenDao

    companion object {
        const val DB_NAME = "safe_db"
        const val LATEST_DB_VERSION = 3

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${Erc20Token.TABLE_NAME}`
                        (`${Erc20Token.COL_ADDRESS}` TEXT NOT NULL,
                        `${Erc20Token.COL_NAME}` TEXT NOT NULL,
                        `${Erc20Token.COL_SYMBOL}` TEXT NOT NULL,
                        `${Erc20Token.COL_DECIMALS}` INTEGER NOT NULL,
                        `${Erc20Token.COL_LOGO_URL}` TEXT NOT NULL,
                        PRIMARY KEY(`${Erc20Token.COL_ADDRESS}`))"""
                )
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${Erc20Token.TABLE_NAME}`
                        (`${Erc20Token.COL_ADDRESS}` TEXT NOT NULL,
                        `${Erc20Token.COL_NAME}` TEXT NOT NULL,
                        `${Erc20Token.COL_SYMBOL}` TEXT NOT NULL,
                        `${Erc20Token.COL_DECIMALS}` INTEGER NOT NULL,
                        `${Erc20Token.COL_LOGO_URL}` TEXT NOT NULL,
                        PRIMARY KEY(`${Erc20Token.COL_ADDRESS}`))"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${SafeMetaData.TABLE_NAME}`
                        (`${SafeMetaData.COL_ADDRESS}` TEXT NOT NULL,
                        `${SafeMetaData.COL_REGISTERED_NOTIFICATIONS}` INTEGER NOT NULL,
                        PRIMARY KEY(`${SafeMetaData.COL_ADDRESS}`),
                        FOREIGN KEY(`${SafeMetaData.COL_ADDRESS}`) REFERENCES `${Safe.TABLE_NAME}`(`${Safe.COL_ADDRESS}`)
                        ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${SafeMetaData.TABLE_NAME}`
                        (`${SafeMetaData.COL_ADDRESS}` TEXT NOT NULL,
                        `${SafeMetaData.COL_REGISTERED_NOTIFICATIONS}` INTEGER NOT NULL,
                        PRIMARY KEY(`${SafeMetaData.COL_ADDRESS}`),
                        FOREIGN KEY(`${SafeMetaData.COL_ADDRESS}`) REFERENCES `${Safe.TABLE_NAME}`(`${Safe.COL_ADDRESS}`)
                        ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)"""
                )
            }
        }
    }
}
