package io.gnosis.data.repositories

import android.app.Application
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.Safe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pm.gnosis.ethereum.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class SafeRepositoryTest {

    private val safeDao = mockk<SafeDao>()
    private val ethereumRepository = mockk<EthereumRepository>()

    private lateinit var preferences: TestPreferences
    private lateinit var safeRepository: SafeRepository

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        val preferencesManager = PreferencesManager(application)
        safeRepository = SafeRepository(safeDao, preferencesManager, ethereumRepository)
    }

    @Test
    fun `getSafes - should return safes in the database`() = runBlocking {
        val safes = arrayOf(
            Safe(Solidity.Address(BigInteger.ZERO), "zero"),
            Safe(Solidity.Address(BigInteger.ONE), "one"),
            Safe(Solidity.Address(BigInteger.TEN), "ten")
        )
        coEvery { safeDao.loadAll() } returns safes

        val actual = safeRepository.getSafes()

        assert(actual == safes.asList())
        coVerify(exactly = 1) { safeDao.loadAll() }
    }

    @Test
    fun `addSafe - (Safe) should succeed`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        coEvery { safeDao.insert(any()) } just Runs

        safeRepository.addSafe(safe)

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

        coVerify(exactly = 1) { preferences.putString(SafeRepository.ACTIVE_SAFE, safe.address.asEthereumAddressString()) }
    }

    @Test
    fun `getActiveSafe - (no active safe) should return null`() = runBlocking {
        val actual = safeRepository.getActiveSafe()

        assertEquals(null, actual)

        coVerify(exactly = 1) { preferences.getString(SafeRepository.ACTIVE_SAFE, null) }
    }

    @Test
    fun `getActiveSafe - (with active safe) should return safe`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        coEvery { safeDao.loadByAddress(any()) } returns safe

        safeRepository.setActiveSafe(safe)
        val actual = safeRepository.getActiveSafe()

        assertEquals(safe, actual)
        coVerify(ordering = Ordering.ORDERED) {
            preferences.putString(SafeRepository.ACTIVE_SAFE, safe.address.asEthereumAddressString())
            preferences.getString(SafeRepository.ACTIVE_SAFE, null)
            safeDao.loadByAddress(safe.address)
        }
    }

    @Test
    fun `isValidSafe - (safe with master copy v0_0_2) should return true`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val ethRequest = buildSuccessfulEthRequest(safeAddress, SafeRepository.SAFE_MASTER_COPY_0_0_2)
        coEvery { ethereumRepository.request(any<EthGetStorageAt>()) } returns ethRequest

        val actual = safeRepository.isValidSafe(safeAddress)

        assertEquals(true, actual)
        coVerify(exactly = 1) { ethereumRepository.request(ethRequest) }
    }

    @Test
    fun `isValidSafe - (safe with master copy v0_1_0) should return true`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val ethRequest = buildSuccessfulEthRequest(safeAddress, SafeRepository.SAFE_MASTER_COPY_0_1_0)
        coEvery { ethereumRepository.request(any<EthGetStorageAt>()) } returns ethRequest

        val actual = safeRepository.isValidSafe(safeAddress)

        assertEquals(true, actual)
        coVerify(exactly = 1) { ethereumRepository.request(ethRequest) }
    }

    @Test
    fun `isValidSafe - (safe with master copy v1_0_0) should return true`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val ethRequest = buildSuccessfulEthRequest(safeAddress, SafeRepository.SAFE_MASTER_COPY_1_0_0)
        coEvery { ethereumRepository.request(any<EthGetStorageAt>()) } returns ethRequest

        val actual = safeRepository.isValidSafe(safeAddress)

        assertEquals(true, actual)
        coVerify(exactly = 1) { ethereumRepository.request(ethRequest) }
    }

    @Test
    fun `isValidSafe - (safe with master copy v1_1_1) should return true`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val ethRequest = buildSuccessfulEthRequest(safeAddress, SafeRepository.SAFE_MASTER_COPY_1_1_1)
        coEvery { ethereumRepository.request(any<EthGetStorageAt>()) } returns ethRequest

        val actual = safeRepository.isValidSafe(safeAddress)

        assertEquals(true, actual)
        coVerify(exactly = 1) { ethereumRepository.request(ethRequest) }
    }

    @Test
    fun `isValidSafe - (safe with unknown master copy) should return false`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val ethRequest = buildSuccessfulEthRequest(safeAddress, Solidity.Address(BigInteger.ONE))
        coEvery { ethereumRepository.request(any<EthGetStorageAt>()) } returns ethRequest

        val actual = safeRepository.isValidSafe(safeAddress)

        assertEquals(false, actual)
        coVerify(exactly = 1) { ethereumRepository.request(ethRequest) }
    }

    @Test
    fun `isValidSafe - (safe with mastercopy but request failure) should return throw`() = runBlocking {
        val errorMessage = "Error message"
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val ethRequest = EthGetStorageAt(safeAddress, BigInteger.ZERO, block = Block.LATEST).apply {
            response = EthRequest.Response.Failure(errorMessage)
        }
        coEvery { ethereumRepository.request(any<EthGetStorageAt>()) } returns ethRequest

        val actual = runCatching { safeRepository.isValidSafe(safeAddress) }

        with(actual) {
            assertEquals(true, isFailure)
            val exception = exceptionOrNull()
            assert(exception is RequestFailedException && true == exception.message?.contains(errorMessage))
            coVerify(exactly = 1) { ethereumRepository.request(ethRequest) }
        }
    }

    @Test
    fun `isValidSafe - (safe with mastercopy but request response is null) should return throw`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val ethRequest = EthGetStorageAt(safeAddress, BigInteger.ZERO, block = Block.LATEST).apply {
            response = null
        }
        coEvery { ethereumRepository.request(any<EthGetStorageAt>()) } returns ethRequest

        val actual = runCatching { safeRepository.isValidSafe(safeAddress) }

        with(actual) {
            assertEquals(true, isFailure)
            val exception = exceptionOrNull()
            assert(exception is RequestNotExecutedException && exception.message == "Valid safe check failed")
            coVerify(exactly = 1) { ethereumRepository.request(ethRequest) }
        }
    }

    private fun buildSuccessfulEthRequest(from: Solidity.Address, masterCopy: Solidity.Address) =
        EthGetStorageAt(from, BigInteger.ZERO, block = Block.LATEST).apply {
            response = EthRequest.Response.Success(masterCopy.asEthereumAddressString())
        }

}
