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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeInfoDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.db.models.toDb
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.db.EncryptedByteArray
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

    val encryptedByteArrayConverter = EncryptedByteArray.Converter()

    @Test
    fun migrations_keepData() {

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
        db.insertSafe(safe)
        db.insertPendingSafe(pendingSafe)
        //Prepare for the next version
        db.close()

        // Re-open the database with version 2 and provide MIGRATION_1_2 as the migration process.
        val db2 = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 2, true,
            ApplicationDb.MIGRATION_1_2
        )

        db2.insertSafeInfo(
            TEST_SAFE_ADDRESS.asEthereumAddress()!!,
            TEST_OWNER_ADDRESS.asEthereumAddress()!!,
            encryptedByteArrayConverter.fromStorage("encrypted_key")
        )

        // Re-open the database with version 3 and provide MIGRATION_2_3 as the migration process.
        val db3 = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 3, true,
            ApplicationDb.MIGRATION_2_3
        )

        db3.insertSafeInfo(
            TEST_SAFE_2_ADDRESS,
            TEST_OWNER_ADDRESS.asEthereumAddress()!!,
            encryptedByteArrayConverter.fromStorage("encrypted_key_2"),
            TEST_TOKEN
        )

        // Re-open the database with version 4 and provide MIGRATION_3_4 as the migration process.
        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME, 4, true,
            ApplicationDb.MIGRATION_3_4
        )

        // Get the latest, migrated, version of the database
        // Check that the correct data is in the database
        val migratedDb = getMigratedRoomDatabase()

        val safeObserver = TestObserver<GnosisSafeDb>()
        migratedDb.gnosisSafeDao().loadSafe(safe.address).subscribe(safeObserver)
        safeObserver.assertResult(safe.toDb())

        val pendingSafeObserver = TestObserver<PendingGnosisSafeDb>()
        migratedDb.gnosisSafeDao().loadPendingSafe(pendingSafe.address).subscribe(pendingSafeObserver)
        pendingSafeObserver.assertResult(pendingSafe.toDb())

        val safeInfoObserver = TestObserver<GnosisSafeInfoDb>()
        migratedDb.gnosisSafeDao().loadSafeInfo(safe.address).subscribe(safeInfoObserver)
        safeInfoObserver.assertValueCount(1).assertNoErrors().assertValue {
            assertEquals(TEST_SAFE_ADDRESS.asEthereumAddress()!!, it.safeAddress)
            assertEquals(TEST_OWNER_ADDRESS.asEthereumAddress()!!, it.ownerAddress)
            assertEquals("encrypted_key", encryptedByteArrayConverter.toStorage(it.ownerPrivateKey))
            assertEquals(ERC20Token.ETHER_TOKEN.address, it.paymentTokenAddress)
            assertEquals(ERC20Token.ETHER_TOKEN.symbol, it.paymentTokenSymbol)
            assertEquals(ERC20Token.ETHER_TOKEN.name, it.paymentTokenName)
            assertEquals(ERC20Token.ETHER_TOKEN.decimals, it.paymentTokenDecimals)
            assertEquals(null, it.paymentTokenIcon)
            true
        }

        val safe2InfoObserver = TestObserver<GnosisSafeInfoDb>()
        migratedDb.gnosisSafeDao().loadSafeInfo(TEST_SAFE_2_ADDRESS).subscribe(safe2InfoObserver)
        safe2InfoObserver.assertValueCount(1).assertNoErrors().assertValue {
            assertEquals(TEST_SAFE_2_ADDRESS, it.safeAddress)
            assertEquals(TEST_OWNER_ADDRESS.asEthereumAddress()!!, it.ownerAddress)
            assertEquals("encrypted_key_2", encryptedByteArrayConverter.toStorage(it.ownerPrivateKey))
            assertEquals(TEST_TOKEN.address, it.paymentTokenAddress)
            assertEquals(TEST_TOKEN.symbol, it.paymentTokenSymbol)
            assertEquals(TEST_TOKEN.name, it.paymentTokenName)
            assertEquals(TEST_TOKEN.decimals, it.paymentTokenDecimals)
            assertEquals(TEST_TOKEN.logoUrl, it.paymentTokenIcon)
            true
        }
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

    private fun SupportSQLiteDatabase.insertSafeInfo(
        safeAddress: Solidity.Address, ownerAddress: Solidity.Address, ownerKey: EncryptedByteArray, paymentToken: ERC20Token? = null
    ) {
        val values = ContentValues()
        values.put(GnosisSafeInfoDb.COL_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
        values.put(GnosisSafeInfoDb.COL_OWNER_ADDRESS, ownerAddress.asEthereumAddressString())
        values.put(GnosisSafeInfoDb.COL_OWNER_PRIVATE_KEY, encryptedByteArrayConverter.toStorage(ownerKey))
        paymentToken?.let {
            values.put(GnosisSafeInfoDb.COL_PAYMENT_TOKEN_ADDRESS, paymentToken.address.asEthereumAddressString())
            values.put(GnosisSafeInfoDb.COL_PAYMENT_TOKEN_SYMBOL, paymentToken.symbol)
            values.put(GnosisSafeInfoDb.COL_PAYMENT_TOKEN_NAME, paymentToken.name)
            values.put(GnosisSafeInfoDb.COL_PAYMENT_TOKEN_DECIMALS, paymentToken.decimals)
            values.put(GnosisSafeInfoDb.COL_PAYMENT_TOKEN_ICON, paymentToken.logoUrl)
        }
        insert(GnosisSafeInfoDb.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun SupportSQLiteDatabase.insertSafe(safe: Safe) {
        val values = ContentValues()
        values.put(GnosisSafeDb.COL_ADDRESS, safe.address.asEthereumAddressString())
        insert(GnosisSafeDb.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun SupportSQLiteDatabase.insertPendingSafe(pendingSafe: PendingSafe) {
        val values = ContentValues()
        values.put(PendingGnosisSafeDb.COL_ADDRESS, pendingSafe.address.asEthereumAddressString())
        values.put(PendingGnosisSafeDb.COL_TX_HASH, BigInteger.ZERO.toHexString())
        values.put(PendingGnosisSafeDb.COL_PAYMENT_TOKEN, pendingSafe.paymentToken.asEthereumAddressString())
        values.put(PendingGnosisSafeDb.COL_PAYMENT_AMOUNT, pendingSafe.paymentAmount.toHexString())
        values.put(PendingGnosisSafeDb.COL_IS_FUNDED, pendingSafe.isFunded)
        insert(PendingGnosisSafeDb.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    companion object {
        private const val TEST_DB_NAME = "test-gnosis-safe-db"
        private const val TEST_SAFE_ADDRESS = "0x1"
        private const val TEST_PENDING_SAFE_ADDRESS = "0x2"
        private const val TEST_OWNER_ADDRESS = "0x71De9579cD3857ce70058a1ce19e3d8894f65Ab9"
        private val TEST_SAFE_2_ADDRESS = "0x11".asEthereumAddress()!!
        private val TEST_TOKEN_ADDRESS = "0xa7e15e2e76ab469f8681b576cff168f37aa246ec".asEthereumAddress()!!
        private val TEST_TOKEN = ERC20Token(TEST_TOKEN_ADDRESS, "Test Token", "TT", 10, "")
    }
}

