package io.gnosis.data.db.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.gnosis.data.db.HeimdallDatabase
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Erc20TokenDaoTest {

    private lateinit var heimdallDatabase: HeimdallDatabase
    private lateinit var safeDao: Erc20TokenDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        heimdallDatabase = Room.inMemoryDatabaseBuilder(context, HeimdallDatabase::class.java).build()
        safeDao = heimdallDatabase.erc20TokenDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        heimdallDatabase.close()
    }

//    @Test
//    @Throws(Exception::class)
//    fun insert__single_safe__should_succeed() = runBlocking {
//        val testSafe = Safe(Solidity.Address(BigInteger.ZERO), "zero")
//
//        safeDao.insert(testSafe)
//
//        with(safeDao.loadAll()) {
//            Assert.assertEquals(1, size)
//            Assert.assertEquals(testSafe, get(0))
//        }
//    }
}
