package io.gnosis.data.db

import android.content.ContentValues
import androidx.room.OnConflictStrategy
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.gnosis.data.models.Erc20Token
import io.gnosis.data.models.Safe
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.io.IOException
import java.math.BigInteger

// https://developer.android.com/training/data-storage/room/migrating-db-versions
@RunWith(AndroidJUnit4::class)
class HeimdallDatabaseTest {

    private val addressConverter = SolidityAddressConverter()

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HeimdallDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe_local_name")
        helper.createDatabase(TEST_DB, 1).apply {
            val rowId = insert(Safe.TABLE_NAME, OnConflictStrategy.REPLACE,
                ContentValues().apply {
                    put(Safe.COL_ADDRESS, addressConverter.toHexString(safe.address))
                    put(Safe.COL_LOCAL_NAME, safe.localName)
                })

            assert(rowId >= 0)

            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, HeimdallDatabase.MIGRATION_1_2).apply {
            with(query("SELECT * FROM ${Safe.TABLE_NAME}")) {
                assertEquals(1, count)
                moveToFirst()
                assertEquals(getString(getColumnIndex(Safe.COL_ADDRESS)), safe.address.asEthereumAddressString())
                assertEquals(getString(getColumnIndex(Safe.COL_LOCAL_NAME)), safe.localName)
            }

            with(query("SELECT * FROM ${Erc20Token.TABLE_NAME}")) {
                assert(getColumnIndex(Erc20Token.COL_ADDRESS) >= 0)
                assert(getColumnIndex(Erc20Token.COL_NAME) >= 0)
                assert(getColumnIndex(Erc20Token.COL_DECIMALS) >= 0)
                assert(getColumnIndex(Erc20Token.COL_LOGO_URL) >= 0)
                assert(getColumnIndex(Erc20Token.COL_SYMBOL) >= 0)
            }

            close()
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
