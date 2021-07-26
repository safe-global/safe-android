package io.gnosis.data.repositories

import android.app.Application
import io.gnosis.data.BuildConfig
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
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class SafeRepositoryTest {

    private val safeDao = mockk<SafeDao>()
    private val gatewayApi = mockk<GatewayApi>()

    private lateinit var preferences: TestPreferences
    private lateinit var safeRepository: SafeRepository

    private val defaultChain = Chain.DEFAULT_CHAIN
    private val defaultCurrency = Chain.Currency.DEFAULT_CURRENCY

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
        coEvery { safeDao.loadAllWithChainData() } returns safes.map { SafeWithChainData(it, defaultChain, defaultCurrency) }

        val actual = safeRepository.getSafes()

        assertTrue(actual == safes)
        coVerify(exactly = 1) { safeDao.loadAllWithChainData() }
    }

    @Test
    fun `getSafeBy (address) - should return safe`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val safe = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        coEvery { safeDao.loadByAddressWithChainData(safeAddress) } returns SafeWithChainData(safe, null, null)

        val actual = safeRepository.getSafeBy(safeAddress)

        assertEquals(safe, actual)
        coVerify(exactly = 1) { safeDao.loadByAddressWithChainData(safeAddress) }
    }

    @Test
    fun `getSafeBy (address, chainId) - should return safe`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ZERO)
        val safe = Safe(Solidity.Address(BigInteger.ZERO), "zero")
        coEvery { safeDao.loadByAddressWithChainData(any(), any()) } returns SafeWithChainData(safe, defaultChain, defaultCurrency)

        val actual = safeRepository.getSafeBy(safeAddress, defaultChain.chainId)

        assertEquals(safe, actual)
        coVerify(exactly = 1) { safeDao.loadByAddressWithChainData(safeAddress, defaultChain.chainId) }
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

        coVerify(exactly = 1) { preferences.putString(ACTIVE_SAFE, "${safe.address.asEthereumAddressString()};${safe.localName};${safe.chainId}") }
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
        coEvery { safeDao.loadByAddressWithChainData(any(), any()) } returns SafeWithChainData(safe, defaultChain, defaultCurrency)

        safeRepository.setActiveSafe(safe)
        val actual = safeRepository.getActiveSafe()

        assertEquals(safe, actual)
        coVerify(ordering = Ordering.ORDERED) {
            preferences.putString(ACTIVE_SAFE, "${safe.address.asEthereumAddressString()};${safe.localName};${safe.chainId}")
            preferences.getString(ACTIVE_SAFE, null)
            safeDao.loadByAddressWithChainData(safe.address, safe.chainId)
        }
    }

    @Test
    fun `isValidSafe - valid safe`() = runBlocking {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val safeInfo = SafeInfo(
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            BigInteger.TEN,
            1,
            listOf(
                AddressInfo(Solidity.Address(BigInteger.ONE))
            ),
            AddressInfo(SAFE_IMPLEMENTATION_1_0_0),
            listOf(AddressInfo(Solidity.Address(BigInteger.ONE))),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            null,
            "1.1.1",
            VersionState.OUTDATED
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
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            BigInteger.TEN,
            1,
            listOf(
                AddressInfo(Solidity.Address(BigInteger.ONE))
            ),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            listOf(AddressInfo(Solidity.Address(BigInteger.ONE))),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            null,
            "1.1.1",
            VersionState.OUTDATED
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
        coEvery { safeDao.loadAllWithChainData() } returns safes.map { SafeWithChainData(it, defaultChain, defaultCurrency) }
        coEvery { safeDao.delete(any()) } just Runs

        safeRepository.clearUserData()

        coVerify(exactly = 1) { safeDao.delete(safes[0]) }
        coVerify(exactly = 1) { safeDao.delete(safes[1]) }
        coVerify(exactly = 1) { safeDao.delete(safes[2]) }
        coVerify(exactly = 1) { preferences.remove(ACTIVE_SAFE) }
    }

    companion object {
        private const val ACTIVE_SAFE = "prefs.string.active_safe"
        private val CHAIN_ID = BuildConfig.CHAIN_ID.toBigInteger()

        private val SAFE_IMPLEMENTATION_1_0_0 = "0x8942595A2dC5181Df0465AF0D7be08c8f23C93af".asEthereumAddress()!!
        private val SAFE_IMPLEMENTATION_1_1_1 = "0xb6029EA3B2c51D09a50B53CA8012FeEB05bDa35A".asEthereumAddress()!!
        private val SAFE_IMPLEMENTATION_1_2_0 = "0x6851d6fdfafd08c0295c392436245e5bc78b0185".asEthereumAddress()!!
        private val SAFE_IMPLEMENTATION_1_3_0 = "0xd9Db270c1B5E3Bd161E8c8503c55cEABeE709552".asEthereumAddress()!!
    }
}
