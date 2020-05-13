package io.gnosis.data.db.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.gnosis.data.db.HeimdallDatabase
import io.gnosis.data.models.Erc20Token
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.model.Solidity
import java.io.IOException
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class Erc20TokenDaoTest {

    private lateinit var heimdallDatabase: HeimdallDatabase
    private lateinit var erc20TokenDao: Erc20TokenDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        heimdallDatabase = Room.inMemoryDatabaseBuilder(context, HeimdallDatabase::class.java).build()
        erc20TokenDao = heimdallDatabase.erc20TokenDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        heimdallDatabase.close()
    }

    @Test
    @Throws(Exception::class)
    fun insert__single_erc20Token__should_succeed() = runBlocking {
        val testToken = buildFakeToken(1)

        erc20TokenDao.insertToken(testToken)

        with(erc20TokenDao.loadTokens()) {
            assertEquals(1, size)
            assertEquals(testToken, get(0))
        }
    }

    @Test
    @Throws(Exception::class)
    fun insert__different_address__should_succeed() = runBlocking {
        val testToken1 = buildFakeToken(1)
        val testToken2 = buildFakeToken(2)

        erc20TokenDao.insertToken(testToken1)
        erc20TokenDao.insertToken(testToken2)

        with(erc20TokenDao.loadTokens()) {
            assertEquals(2, size)
            assertEquals(testToken1, get(0))
            assertEquals(testToken2, get(1))
        }
    }

    @Test
    @Throws(Exception::class)
    fun insert__same_address__should_succeed() = runBlocking {
        val testToken1 = buildFakeToken(1)
        val testToken2 = buildFakeToken(2).copy(address = testToken1.address)

        erc20TokenDao.insertToken(testToken1)
        erc20TokenDao.insertToken(testToken2)

        with(erc20TokenDao.loadTokens()) {
            assertEquals(1, size)
            assertEquals(testToken2, get(0))
        }
    }

    @Test
    @Throws(Exception::class)
    fun insert__multiple__should_succeed() = runBlocking {
        val testToken1 = buildFakeToken(1)
        val testToken2 = buildFakeToken(2)

        erc20TokenDao.insertTokens(listOf(testToken1, testToken2))

        with(erc20TokenDao.loadTokens()) {
            assertEquals(2, size)
            assertEquals(testToken1, get(0))
            assertEquals(testToken2, get(1))
        }
    }

    @Test
    @Throws(Exception::class)
    fun insert__multiple_overlapping__should_replace() = runBlocking {
        val testToken1 = buildFakeToken(1)
        val testToken2 = buildFakeToken(2)
        val testToken3 = buildFakeToken(3)
        val testToken4 = buildFakeToken(4)

        val testToken5 = buildFakeToken(5).copy(address = testToken2.address)
        val testToken6 = buildFakeToken(6).copy(address = testToken3.address)

        erc20TokenDao.insertTokens(listOf(testToken1, testToken2, testToken3, testToken4))
        erc20TokenDao.insertTokens(listOf(testToken5, testToken6))

        with(erc20TokenDao.loadTokens()) {
            assertEquals(4, size)
            assertEquals(testToken1, get(0))
            assertEquals(testToken4, get(1))
            assertEquals(testToken5, get(2))
            assertEquals(testToken6, get(3))
        }
    }

    @Test
    @Throws(Exception::class)
    fun loadTokens__multiple__should_return_list() = runBlocking {
        val testToken1 = buildFakeToken(1)
        val testToken2 = buildFakeToken(2)
        val testToken3 = buildFakeToken(3)

        erc20TokenDao.insertTokens(listOf(testToken1, testToken2,testToken3))

        with(erc20TokenDao.loadTokens()) {
            assertEquals(3, size)
            assertEquals(testToken1, get(0))
            assertEquals(testToken2, get(1))
            assertEquals(testToken3, get(2))
        }
    }

    @Test
    @Throws(Exception::class)
    fun loadToken__inserted_address__should_return_token() = runBlocking {
        val testToken1 = buildFakeToken(1)
        val testToken2 = buildFakeToken(2)
        val testToken3 = buildFakeToken(3)

        erc20TokenDao.insertTokens(listOf(testToken1, testToken2,testToken3))

        with(erc20TokenDao.loadToken(testToken2.address)) {
            assertEquals(testToken2, this)
        }
    }

    @Test
    @Throws(Exception::class)
    fun loadToken__missing_address__should_return_null() = runBlocking {
        val testToken1 = buildFakeToken(1)
        val testToken2 = buildFakeToken(2)
        val testToken3 = buildFakeToken(3)

        erc20TokenDao.insertTokens(listOf(testToken1, testToken2,testToken3))

        with(erc20TokenDao.loadToken(Solidity.Address(BigInteger.ZERO))) {
            assertEquals(null, this)
        }
    }

    @Test
    @Throws(Exception::class)
    fun deleteToken__inserted_address__should_succeed() = runBlocking {
        val testToken1 = buildFakeToken(1)

        erc20TokenDao.insertToken(testToken1)
        erc20TokenDao.deleteToken(testToken1.address)

        with(erc20TokenDao.loadTokens()) {
            assertEquals(0, size)
        }
    }

    @Test
    @Throws(Exception::class)
    fun deleteToken__missing_address__should_do_nothing() = runBlocking {
        val testToken1 = buildFakeToken(1)

        erc20TokenDao.insertToken(testToken1)
        erc20TokenDao.deleteToken(Solidity.Address(BigInteger.ZERO))

        with(erc20TokenDao.loadTokens()) {
            assertEquals(1, size)
            assertEquals(testToken1, get(0))
        }
    }


    private fun buildFakeToken(index: Long) =
        Erc20Token(
            Solidity.Address(BigInteger.valueOf(index)),
            "token$index",
            "symbol_$index",
            15,
            "log.url.$index"
        )
}
