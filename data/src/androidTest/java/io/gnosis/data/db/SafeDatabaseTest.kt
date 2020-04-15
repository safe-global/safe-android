package io.gnosis.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.model.Solidity
import java.io.IOException
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class SafeDatabaseTest {

    private lateinit var safeDatabase: SafeDatabase
    private lateinit var safeDao: SafeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        safeDatabase = Room.inMemoryDatabaseBuilder(context, SafeDatabase::class.java).build()
        safeDao = safeDatabase.safeDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        safeDatabase.close()
    }

    @Test
    @Throws(Exception::class)
    fun insert__single_safe__should_succeed() = runBlocking {
        val testSafe = Safe(Solidity.Address(BigInteger.ZERO), "zero")

        safeDao.insert(testSafe)

        with(safeDao.loadAll()) {
            assertEquals(1, size)
            assertEquals(testSafe, get(0))
        }
    }

    @Test
    @Throws(Exception::class)
    fun insert__2_safes_different__address__should_replace_existent() = runBlocking {
        val testSafe1 = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        val testSafe2 = Safe(Solidity.Address(BigInteger.ONE), "one")

        safeDao.insert(testSafe1)
        safeDao.insert(testSafe2)

        with(safeDao.loadAll()) {
            assertEquals(2, size)
            assertEquals(testSafe1, get(0))
            assertEquals(testSafe2, get(1))
        }
    }

    @Test
    @Throws(Exception::class)
    fun insert__2_safe_same_address__should_replace_existent() = runBlocking {
        val testSafe1 = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        val testSafe2 = Safe(Solidity.Address(BigInteger.ZERO), "one")

        safeDao.insert(testSafe1)
        safeDao.insert(testSafe2)

        with(safeDao.loadAll()) {
            assertEquals(1, size)
            assertEquals(testSafe2, get(0))
        }
    }

    @Test
    @Throws(Exception::class)
    fun loadAll__3_safes__should_return_list_of_3() = runBlocking {
        val testSafe1 = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        val testSafe2 = Safe(Solidity.Address(BigInteger.ONE), "one")
        val testSafe3 = Safe(Solidity.Address(BigInteger.TEN), "ten")

        safeDao.insert(testSafe1)
        safeDao.insert(testSafe2)
        safeDao.insert(testSafe3)

        with(safeDao.loadAll()) {
            assertEquals(3, size)
            assertEquals(testSafe1, get(0))
            assertEquals(testSafe2, get(1))
            assertEquals(testSafe3, get(2))
        }
    }

    @Test
    @Throws(Exception::class)
    fun remove__safe__should_succeed() = runBlocking {
        val testSafe = Safe(Solidity.Address(BigInteger.ZERO), "zero")

        safeDao.insert(testSafe)
        safeDao.delete(testSafe)

        with(safeDao.loadAll()) {
            assertEquals(0, size)
            assertEquals(true, isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    fun remove__safe_with_same_address_should__succeed() = runBlocking {
        val testSafe1 = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        val testSafe2 = Safe(Solidity.Address(BigInteger.ZERO), "one")

        safeDao.insert(testSafe1)
        safeDao.delete(testSafe2)

        with(safeDao.loadAll()) {
            assertEquals(0, size)
            assertEquals(true, isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    fun remove__inexistent_safe__should_do_nothing() = runBlocking {
        val testSafe = Safe(Solidity.Address(BigInteger.ZERO), "zero")

        safeDao.delete(testSafe)

        assertEquals(true, safeDao.loadAll().isEmpty())
    }
}
