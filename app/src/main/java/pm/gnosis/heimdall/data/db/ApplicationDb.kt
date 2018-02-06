package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import pm.gnosis.heimdall.data.db.daos.AddressBookDao
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.db.daos.ERC20TokenDao
import pm.gnosis.heimdall.data.db.daos.GnosisSafeDao
import pm.gnosis.heimdall.data.db.models.*

@Database(entities = [
    AddressBookEntryDb::class,
    ERC20TokenDb::class,
    GnosisSafeDb::class,
    PendingGnosisSafeDb::class,
    TransactionDescriptionDb::class,
    TransactionPublishStatusDb::class
], version = 1)
@TypeConverters(BigIntegerConverter::class)
abstract class ApplicationDb : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-safe-db"
    }

    abstract fun addressBookDao(): AddressBookDao
    abstract fun descriptionsDao(): DescriptionsDao
    abstract fun erc20TokenDao(): ERC20TokenDao
    abstract fun gnosisSafeDao(): GnosisSafeDao
}
