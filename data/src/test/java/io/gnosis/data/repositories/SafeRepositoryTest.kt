package io.gnosis.data.repositories

import android.app.Application
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.SafeInfoDto
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class SafeRepositoryTest {

    private val safeDao = mockk<SafeDao>()
    private val transactionServiceApi = mockk<TransactionServiceApi>()

    private lateinit var preferences: TestPreferences
    private lateinit var safeRepository: SafeRepository

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        val preferencesManager = PreferencesManager(application)
        safeRepository = SafeRepository(safeDao, preferencesManager, transactionServiceApi)
    }

    @Test
    fun `getSafes - should return safes in the database`() = runBlocking {
        val safes = listOf(
            Safe(Solidity.Address(BigInteger.ZERO), "zero"),
            Safe(Solidity.Address(BigInteger.ONE), "one"),
            Safe(Solidity.Address(BigInteger.TEN), "ten")
        )
        coEvery { safeDao.loadAll() } returns safes

        val actual = safeRepository.getSafes()

        assert(actual == safes)
        coVerify(exactly = 1) { safeDao.loadAll() }
    }

    @Test
    fun `addSafe - (Safe) should succeed`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        coEvery { safeDao.insert(any()) } just Runs

        safeRepository.saveSafe(safe)

        coVerify(exactly = 1) { safeDao.insert(safe) }
    }

    @Test
    fun `removeSafe - (Safe) should succeed`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        coEvery { safeDao.delete(safe) } just Runs

        safeRepository.removeSafe(safe)

        coVerify(exactly = 1) { safeDao.delete(safe) }
    }

    @Test
    fun `setActiveSafe - (Safe) should update active safe`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ZERO), "zero")

        safeRepository.setActiveSafe(safe)

        coVerify(exactly = 1) { preferences.putString(ACTIVE_SAFE, "${safe.address.asEthereumAddressString()};${safe.localName}") }
    }

    @Test
    fun `clearActiveSafe - (Safe) should update active safe to null`() = runBlocking {
        safeRepository.clearActiveSafe()

        coVerify(exactly = 1) { preferences.remove(ACTIVE_SAFE) }
    }

    @Test
    fun `getActiveSafe - (no active safe) should return null`() = runBlocking {
        val actual = safeRepository.getActiveSafe()

        assertEquals(null, actual)

        coVerify(exactly = 1) { preferences.getString(ACTIVE_SAFE, null) }
    }

    @Test
    fun `getActiveSafe - (with active safe) should return safe`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        coEvery { safeDao.loadByAddress(any()) } returns safe

        safeRepository.setActiveSafe(safe)
        val actual = safeRepository.getActiveSafe()

        assertEquals(safe, actual)
        coVerify(ordering = Ordering.ORDERED) {
            preferences.putString(ACTIVE_SAFE, "${safe.address.asEthereumAddressString()};${safe.localName}")
            preferences.getString(ACTIVE_SAFE, null)
            safeDao.loadByAddress(safe.address)
        }
    }

    @Test
    fun `isValidSafe - valid safe`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val safeInfoDto = SafeInfoDto(
            Solidity.Address(BigInteger.ONE),
            BigInteger.TEN,
            1,
            listOf(Solidity.Address(BigInteger.ONE)),
            Solidity.Address(BigInteger.ONE),
            listOf(Solidity.Address(BigInteger.ONE)),
            Solidity.Address(BigInteger.ONE),
            "v1"
        )
        coEvery { transactionServiceApi.getSafeInfo(any()) } returns safeInfoDto

        val actual = safeRepository.isValidSafe(safeAddress)

        assertEquals(true, actual)
        coVerify(exactly = 1) { transactionServiceApi.getSafeInfo(safeAddress.asEthereumAddressChecksumString()) }
    }

    @Test
    fun `isValidSafe - invalid safe`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val throwable = Throwable()
        coEvery { transactionServiceApi.getSafeInfo(any()) } throws throwable

        val actual = safeRepository.isValidSafe(safeAddress)

        assertEquals(false, actual)
        coVerify(exactly = 1) { transactionServiceApi.getSafeInfo(safeAddress.asEthereumAddressChecksumString()) }
    }

    @Test
    fun `isSafeAddressUsed - (contained address) should return true`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        coEvery { safeDao.loadByAddress(any()) } returns Safe(safeAddress, "safe_name")

        val actual = safeRepository.isSafeAddressUsed(safeAddress)

        assertEquals(true, actual)
        coVerify(exactly = 1) { safeDao.loadByAddress(safeAddress) }
    }

    @Test
    fun `isSafeAddressUsed - (new address) should return false`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        coEvery { safeDao.loadByAddress(any()) } returns null

        val actual = safeRepository.isSafeAddressUsed(safeAddress)

        assertEquals(false, actual)
        coVerify(exactly = 1) { safeDao.loadByAddress(safeAddress) }
    }

    @Test
    fun `isSafeAddressUsed - (DAO failure) should throw`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val throwable = Throwable()
        coEvery { safeDao.loadByAddress(any()) } throws throwable

        val actual = runCatching { safeRepository.isSafeAddressUsed(safeAddress) }

        with(actual) {
            assertEquals(true, isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { safeDao.loadByAddress(safeAddress) }
    }

    @Test
    fun `getSafeInfo - (transaction service failure) should throw`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val throwable = Throwable()
        coEvery { transactionServiceApi.getSafeInfo(any()) } throws throwable

        val actual = runCatching { safeRepository.getSafeInfo(safeAddress) }

        with(actual) {
            assertEquals(true, isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { transactionServiceApi.getSafeInfo(safeAddress.asEthereumAddressString()) }
    }

    @Test
    fun `getSafeInfo - (valid address) should return safeInfo`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val safeInfoDto = SafeInfoDto(
            Solidity.Address(BigInteger.ONE),
            BigInteger.TEN,
            1,
            listOf(Solidity.Address(BigInteger.ONE)),
            Solidity.Address(BigInteger.ONE),
            listOf(Solidity.Address(BigInteger.ONE)),
            Solidity.Address(BigInteger.ONE),
            "v1"
        )
        coEvery { transactionServiceApi.getSafeInfo(any()) } returns safeInfoDto

        val actual = runCatching { safeRepository.getSafeInfo(safeAddress) }

        with(actual) {
            assertEquals(true, isSuccess)
            val safeInfo = getOrNull()
            assertEquals(safeInfoDto.address, safeInfo?.address)
            assertEquals(safeInfoDto.nonce, safeInfo?.nonce)
            assertEquals(safeInfoDto.threshold, safeInfo?.threshold)
        }
        coVerify(exactly = 1) { transactionServiceApi.getSafeInfo(safeAddress.asEthereumAddressString()) }
    }

    companion object {
        private const val ACTIVE_SAFE = "prefs.string.active_safe"
    }
}
