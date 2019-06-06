package pm.gnosis.heimdall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pm.gnosis.heimdall.data.db.daos.AddressBookDao
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.db.daos.ERC20TokenDao
import pm.gnosis.heimdall.data.db.daos.GnosisSafeDao
import pm.gnosis.heimdall.data.db.models.*
import pm.gnosis.svalinn.security.db.EncryptedByteArray

@Database(
    entities = [
        AddressBookEntryDb::class,
        ERC20TokenDb::class,
        GnosisSafeOwnerDb::class,
        GnosisSafeDb::class,
        PendingGnosisSafeDb::class,
        RecoveringGnosisSafeDb::class,
        TransactionDescriptionDb::class,
        TransactionPublishStatusDb::class
    ], version = 2
)
@TypeConverters(BigIntegerConverter::class, SolidityAddressConverter::class, WeiConverter::class, EncryptedByteArray.Converter::class)
abstract class ApplicationDb : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-safe-db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE `${GnosisSafeOwnerDb.TABLE_NAME}`
                            (`${GnosisSafeOwnerDb.COL_PRIVATE_KEY}` TEXT,
                            `${GnosisSafeOwnerDb.COL_ADDRESS}` TEXT,
                            `${GnosisSafeOwnerDb.COL_SAFE_ADDRESS}` TEXT,
                            PRIMARY KEY(`${GnosisSafeOwnerDb.COL_PRIVATE_KEY}`))"""
                )
            }
        }

    }

    abstract fun addressBookDao(): AddressBookDao
    abstract fun descriptionsDao(): DescriptionsDao
    abstract fun erc20TokenDao(): ERC20TokenDao
    abstract fun gnosisSafeDao(): GnosisSafeDao
}
