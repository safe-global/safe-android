package io.gnosis.data.repositories

import android.app.Application
import io.gnosis.data.BuildConfig.CHAIN_ID
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class SafeRepositoryTest {

    private val safeDao = mockk<SafeDao>()
    private val gatewayApi = mockk<GatewayApi>()

    private lateinit var preferences: TestPreferences
    private lateinit var safeRepository: SafeRepository

    private var defaultChain = Chain(CHAIN_ID, "Name", "", "")

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        val preferencesManager = PreferencesManager(application)
        safeRepository = SafeRepository(safeDao, preferencesManager, gatewayApi)
    }

    @Test
    fun `getSafes - should return safes in the database`() = runBlocking {
        val safes = listOf(
            Safe(Solidity.Address(BigInteger.ZERO), "zero"),
            Safe(Solidity.Address(BigInteger.ONE), "one"),
            Safe(Solidity.Address(BigInteger.TEN), "ten")
        )
        val chain = Chain(1, "chain", "", "")
        coEvery { safeDao.loadAllWithChainData() } returns safes.map { SafeWithChainData(it, chain) }

        val actual = safeRepository.getSafes()

        assertTrue(actual == safes)
        coVerify(exactly = 1) { safeDao.loadAllWithChainData() }
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
        val chain = Chain(1, "chain", "", "")
        coEvery { safeDao.loadByAddressWithChainData(any()) } returns SafeWithChainData(safe, chain)

        safeRepository.setActiveSafe(safe)
        val actual = safeRepository.getActiveSafe()

        assertEquals(safe, actual)
        coVerify(ordering = Ordering.ORDERED) {
            preferences.putString(ACTIVE_SAFE, "${safe.address.asEthereumAddressString()};${safe.localName}")
            preferences.getString(ACTIVE_SAFE, null)
            safeDao.loadByAddressWithChainData(safe.address)
        }
    }

    @Test
    fun `isValidSafe - valid safe`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val safeInfo = SafeInfo(
            AddressInfoExtended(Solidity.Address(BigInteger.ONE)),
            BigInteger.TEN,
            1,
            listOf(
                AddressInfoExtended(Solidity.Address(BigInteger.ONE))
            ),
            AddressInfoExtended(SafeRepository.SAFE_IMPLEMENTATION_1_0_0),
            listOf(AddressInfoExtended(Solidity.Address(BigInteger.ONE))),
            AddressInfoExtended(Solidity.Address(BigInteger.ONE)),
            "v1"
        )

        coEvery { gatewayApi.getSafeInfo(address = any(), chainId = any()) } returns safeInfo

        val actual = safeRepository.getSafeStatus(Safe(safeAddress, "", defaultChain.chainId))

        assertEquals(SafeStatus.VALID, actual)
        coVerify(exactly = 1) { gatewayApi.getSafeInfo(address = safeAddress.asEthereumAddressChecksumString(), chainId = CHAIN_ID) }
    }

    @Test
    fun `isValidSafe - invalid safe`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val throwable = Throwable()
        coEvery { gatewayApi.getSafeInfo(address = any(), chainId = any()) } throws throwable

        kotlin.runCatching {
            safeRepository.getSafeStatus(Safe(safeAddress, "", defaultChain.chainId))
            Assert.fail()
        }

        coVerify(exactly = 1) { gatewayApi.getSafeInfo(address = safeAddress.asEthereumAddressChecksumString(), chainId = CHAIN_ID) }
    }

    @Test
    fun `isSafeAddressUsed - (contained address) should return true`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        coEvery { safeDao.loadByAddressAndChainId(any(), any()) } returns Safe(safeAddress, "safe_name")

        val actual = safeRepository.isSafeAddressUsed(Safe(safeAddress, "", defaultChain.chainId))

        assertEquals(true, actual)
        coVerify(exactly = 1) { safeDao.loadByAddressAndChainId(safeAddress, CHAIN_ID) }
    }

    @Test
    fun `isSafeAddressUsed - (new address) should return false`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        coEvery { safeDao.loadByAddressAndChainId(any(), any()) } returns null

        val actual = safeRepository.isSafeAddressUsed(Safe(safeAddress, "", defaultChain.chainId))

        assertEquals(false, actual)
        coVerify(exactly = 1) { safeDao.loadByAddressAndChainId(safeAddress, CHAIN_ID) }
    }

    @Test
    fun `isSafeAddressUsed - (DAO failure) should throw`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val throwable = Throwable()
        coEvery { safeDao.loadByAddressAndChainId(any(), any()) } throws throwable

        val actual = runCatching { safeRepository.isSafeAddressUsed(Safe(safeAddress, "", defaultChain.chainId)) }

        with(actual) {
            assertEquals(true, isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { safeDao.loadByAddressAndChainId(safeAddress, CHAIN_ID) }
    }

    @Test
    fun `getSafeInfo - (transaction service failure) should throw`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "Name", CHAIN_ID)
        val throwable = Throwable()
        coEvery { gatewayApi.getSafeInfo(address = any(), chainId = any()) } throws throwable

        val actual = runCatching { safeRepository.getSafeInfo(safe) }

        with(actual) {
            assertEquals(true, isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { gatewayApi.getSafeInfo(address = safe.address.asEthereumAddressString(), chainId = CHAIN_ID) }
    }

    @Test
    fun `getSafeInfo - (valid address) should return safeInfo`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "Name", CHAIN_ID)
        val safeInfo = SafeInfo(
            AddressInfoExtended(Solidity.Address(BigInteger.ONE)),
            BigInteger.TEN,
            1,
            listOf(
                AddressInfoExtended(Solidity.Address(BigInteger.ONE))
            ),
            AddressInfoExtended(Solidity.Address(BigInteger.ONE)),
            listOf(AddressInfoExtended(Solidity.Address(BigInteger.ONE))),
            AddressInfoExtended(Solidity.Address(BigInteger.ONE)),
            "v1"
        )
        coEvery { gatewayApi.getSafeInfo(address = any(), chainId = any()) } returns safeInfo

        val actual = runCatching { safeRepository.getSafeInfo(safe) }

        with(actual) {
            assertEquals(true, isSuccess)
            val result = getOrNull()
            assertEquals(safeInfo.address, result?.address)
            assertEquals(safeInfo.nonce, result?.nonce)
            assertEquals(safeInfo.threshold, result?.threshold)
        }
        coVerify(exactly = 1) { gatewayApi.getSafeInfo(address = safe.address.asEthereumAddressString(), chainId = CHAIN_ID) }
    }

    @Test
    fun `clearUserData - (Safe) should succeed`() = runBlocking {
        val safes = listOf(
            Safe(Solidity.Address(BigInteger.ZERO), "zero"),
            Safe(Solidity.Address(BigInteger.ONE), "one"),
            Safe(Solidity.Address(BigInteger.TEN), "ten")
        )
        val chain = Chain(1, "chain", "", "")
        coEvery { safeDao.loadAllWithChainData() } returns safes.map { SafeWithChainData(it, chain) }
        coEvery { safeDao.delete(any()) } just Runs

        safeRepository.clearUserData()

        coVerify(exactly = 1) { safeDao.delete(safes[0]) }
        coVerify(exactly = 1) { safeDao.delete(safes[1]) }
        coVerify(exactly = 1) { safeDao.delete(safes[2]) }
        coVerify(exactly = 1) { preferences.remove(ACTIVE_SAFE) }
    }

    companion object {
        private const val ACTIVE_SAFE = "prefs.string.active_safe"
    }
}
