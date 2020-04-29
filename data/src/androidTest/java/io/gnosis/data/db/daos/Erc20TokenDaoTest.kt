package io.gnosis.data.db.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.gnosis.data.db.HeimdallDatabase
import io.gnosis.data.models.Erc20Token
import io.gnosis.data.models.Safe
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

        erc20TokenDao.insertERC20Token(testToken)

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

        erc20TokenDao.insertERC20Token(testToken1)
        erc20TokenDao.insertERC20Token(testToken2)

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

        erc20TokenDao.insertERC20Token(testToken1)
        erc20TokenDao.insertERC20Token(testToken2)

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

        erc20TokenDao.insertERC20Tokens(listOf(testToken1, testToken2))

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

        erc20TokenDao.insertERC20Tokens(listOf(testToken1, testToken2, testToken3, testToken4))
        erc20TokenDao.insertERC20Tokens(listOf(testToken5, testToken6))

        with(erc20TokenDao.loadTokens()) {
            assertEquals(4, size)
            assertEquals(testToken1, get(0))
            assertEquals(testToken4, get(1))
            assertEquals(testToken5, get(2))
            assertEquals(testToken6, get(3))
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
