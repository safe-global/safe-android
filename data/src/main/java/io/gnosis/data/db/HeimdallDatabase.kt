package io.gnosis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        const val LATEST_DB_VERSION = 1
    }
}
