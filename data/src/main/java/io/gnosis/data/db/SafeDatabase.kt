package io.gnosis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe

@Database(entities = [Safe::class], version = 1)
@TypeConverters(SolidityAddressConverter::class)
abstract class SafeDatabase : RoomDatabase() {

    abstract fun safeDao(): SafeDao

    companion object {
        const val DB_NAME = "safe_db"
    }
}
