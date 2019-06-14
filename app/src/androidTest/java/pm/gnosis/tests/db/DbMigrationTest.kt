package pm.gnosis.tests.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.reactivex.observers.TestObserver
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.db.models.toDb
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.toHexString
import java.math.BigInteger


@RunWith(AndroidJUnit4::class)
class DbMigrationTest {

    // Helper for creating Room databases and migrations
    @JvmField
    @Rule
    val migrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            ApplicationDb::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migrationFrom1To2_keepsData() {

        val safe = Safe(TEST_SAFE_ADDRESS.asEthereumAddress()!!)

        val pendingSafe = PendingSafe(
            TEST_PENDING_SAFE_ADDRESS.asEthereumAddress()!!,
            "0x0".asEthereumAddress()!!,
            BigInteger.valueOf(0),
            false
        )

        // Create the database in version 1
        val db = migrationTestHelper.createDatabase(TEST_DB_NAME, 1)
        // insert data
        insertSafe(safe, db)
        insertPendingSafe(pendingSafe, db)
        //Prepare for the next version
        db.close()

        // Re-open the database with version 2 and provide MIGRATION_1_2 as the migration process.
        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 2, true,
            ApplicationDb.MIGRATION_1_2
        )

        // Get the latest, migrated, version of the database
        // Check that the correct data is in the database
        val safeObserver = TestObserver<GnosisSafeDb>()
        getMigratedRoomDatabase().gnosisSafeDao().loadSafe(safe.address).subscribe(safeObserver)
        safeObserver.assertResult(
            safe.toDb()
        )

        val pendingSafeObserver = TestObserver<PendingGnosisSafeDb>()
        getMigratedRoomDatabase().gnosisSafeDao().loadPendingSafe(pendingSafe.address).subscribe(pendingSafeObserver)
        pendingSafeObserver.assertResult(
            pendingSafe.toDb()
        )
    }

    private fun getMigratedRoomDatabase(): ApplicationDb {
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ApplicationDb::class.java, TEST_DB_NAME
        )
            .addMigrations(ApplicationDb.MIGRATION_1_2)
            .build()
        // close the database and release any stream resources when the test finishes
        migrationTestHelper.closeWhenFinished(database)
        return database
    }

    private fun insertSafe(safe: Safe, db: SupportSQLiteDatabase) {
        val values = ContentValues()
        values.put(GnosisSafeDb.COL_ADDRESS, safe.address.asEthereumAddressString())
        db.insert(GnosisSafeDb.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun insertPendingSafe(pendingSafe: PendingSafe, db: SupportSQLiteDatabase) {
        val values = ContentValues()
        values.put(PendingGnosisSafeDb.COL_ADDRESS, pendingSafe.address.asEthereumAddressString())
        values.put(PendingGnosisSafeDb.COL_TX_HASH, BigInteger.ZERO.toHexString())
        values.put(PendingGnosisSafeDb.COL_PAYMENT_TOKEN, pendingSafe.paymentToken.asEthereumAddressString())
        values.put(PendingGnosisSafeDb.COL_PAYMENT_AMOUNT, pendingSafe.paymentAmount.toHexString())
        values.put(PendingGnosisSafeDb.COL_IS_FUNDED, pendingSafe.isFunded)
        db.insert(PendingGnosisSafeDb.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    companion object {
        const val TEST_DB_NAME = "test-gnosis-safe-db"
        const val TEST_SAFE_ADDRESS = "0x1"
        const val TEST_PENDING_SAFE_ADDRESS = "0x2"
    }
}

