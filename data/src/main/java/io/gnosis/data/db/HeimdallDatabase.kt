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

@Database(
    entities = [
        Safe::class,
        Erc20Token::class
    ], version = HeimdallDatabase.LATEST_DB_VERSION
)
@TypeConverters(SolidityAddressConverter::class)
abstract class HeimdallDatabase : RoomDatabase() {

    abstract fun safeDao(): SafeDao

    abstract fun erc20TokenDao(): Erc20TokenDao

    companion object {
        const val DB_NAME = "safe_db"
        const val LATEST_DB_VERSION = 2

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
    }
}
