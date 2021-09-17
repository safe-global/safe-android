package io.gnosis.data.db.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import io.gnosis.data.BuildConfig
import io.gnosis.data.db.HeimdallDatabase
import io.gnosis.data.models.Safe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.model.Solidity
import java.io.IOException
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class SafeDaoTest {

    private lateinit var heimdallDatabase: HeimdallDatabase
    private lateinit var safeDao: SafeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        heimdallDatabase = Room.inMemoryDatabaseBuilder(context, HeimdallDatabase::class.java).build()
        safeDao = heimdallDatabase.safeDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        heimdallDatabase.close()
    }

    @Test
    @Throws(Exception::class)
    fun insert__single_safe__should_succeed() = runBlocking {
        val testSafe = Safe(Solidity.Address(BigInteger.ZERO), "zero")

        safeDao.insert(testSafe)

        with(safeDao.loadAll()) {
            Assert.assertEquals(1, size)
            Assert.assertEquals(testSafe, get(0))
        }
    }

    @Test
    @Throws(Exception::class)
    fun insert__2_safes_different__address__should_insert_both() = runBlocking {
        val testSafe1 = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        val testSafe2 = Safe(Solidity.Address(BigInteger.ONE), "one")

        safeDao.insert(testSafe1)
        safeDao.insert(testSafe2)

        with(safeDao.loadAll()) {
            Assert.assertEquals(2, size)
            Assert.assertEquals(testSafe1, get(0))
            Assert.assertEquals(testSafe2, get(1))
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
            Assert.assertEquals(1, size)
            Assert.assertEquals(testSafe2, get(0))
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
            Assert.assertEquals(3, size)
            Assert.assertEquals(testSafe1, get(0))
            Assert.assertEquals(testSafe2, get(1))
            Assert.assertEquals(testSafe3, get(2))
        }
    }

    @Test
    @Throws(Exception::class)
    fun remove__safe__should_succeed() = runBlocking {
        val testSafe = Safe(Solidity.Address(BigInteger.ZERO), "zero")

        safeDao.insert(testSafe)
        safeDao.delete(testSafe)

        with(safeDao.loadAll()) {
            Assert.assertEquals(0, size)
            Assert.assertEquals(true, isEmpty())
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
            Assert.assertEquals(0, size)
            Assert.assertEquals(true, isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    fun remove__inexistent_safe__should_do_nothing() = runBlocking {
        val testSafe = Safe(Solidity.Address(BigInteger.ZERO), "zero")

        safeDao.delete(testSafe)

        Assert.assertEquals(true, safeDao.loadAll().isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun loadByAddress__safe_address_present__should_return_safe() = runBlocking {
        val testSafe1 = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        val testSafe2 = Safe(Solidity.Address(BigInteger.ONE), "one")
        val testSafe3 = Safe(Solidity.Address(BigInteger.TEN), "ten")

        safeDao.insert(testSafe1)
        safeDao.insert(testSafe2)
        safeDao.insert(testSafe3)

        val actual = safeDao.loadByAddressAndChainId(testSafe2.address, CHAIN_ID)

        Assert.assertEquals(testSafe2, actual)
    }

    @Test
    @Throws(Exception::class)
    fun loadByAddress__safe_address_not_found__should_return_null() = runBlocking {
        val testSafe1 = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        val testSafe2 = Safe(Solidity.Address(BigInteger.ONE), "one")
        val testSafe3 = Safe(Solidity.Address(BigInteger.TEN), "ten")
        val testSafe4 = Safe(Solidity.Address(BigInteger.valueOf(4)), "four")

        safeDao.insert(testSafe1)
        safeDao.insert(testSafe2)
        safeDao.insert(testSafe3)

        val actual = safeDao.loadByAddressAndChainId(testSafe4.address, CHAIN_ID)

        Assert.assertEquals(null, actual)
    }

    companion object {
        private val CHAIN_ID = BuildConfig.CHAIN_ID.toBigInteger()
    }
}
