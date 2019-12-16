package pm.gnosis.heimdall.data.repositories.impls

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.data.repositories.CardRepository
import pm.gnosis.heimdall.helpers.AppPreferencesManager
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.*
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class DefaultCardRepositoryTest {

    private val testPreferences = TestPreferences()

    @Mock
    private lateinit var appPreferencesManager: AppPreferencesManager
    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    private lateinit var repository: DefaultCardRepository

    @Before
    fun setUp() {
        given(appPreferencesManager.get(anyString())).willReturn(testPreferences)
        repository = DefaultCardRepository(encryptionManagerMock, appPreferencesManager)
    }

    @Test
    fun loadKeyCardsEmpty() = runBlockingTest {
        assertEquals(emptyList<CardRepository.CardInfo>(), repository.loadKeyCards())
    }

    @Test
    fun loadKeyCards() = runBlockingTest {
        given(encryptionManagerMock.decrypt(MockUtils.any())).willAnswer {
            val cd = (it.arguments.first() as EncryptionManager.CryptoData)
            cd.data
        }
        testPreferences.putString("testId", "coolstuf".toByteArray().toHex() + "####1a;some_name;1")
        testPreferences.putString("someOtherId", "coolshit".toByteArray().toHex() + "####1b")
        assertEquals(listOf(
            CardRepository.CardInfo("someOtherId", "Unknown card", 0, "coolshit"),
            CardRepository.CardInfo("testId", "some_name", 1, "coolstuf")
        ), repository.loadKeyCards())
    }

    @Test
    fun loadKeyCard() = runBlockingTest {
        given(encryptionManagerMock.decrypt(MockUtils.any())).willAnswer {
            val cd = (it.arguments.first() as EncryptionManager.CryptoData)
            cd.data
        }
        testPreferences.putString("someOtherId", "coolshit".toByteArray().toHex() + "####1b")
        assertEquals(
            CardRepository.CardInfo("someOtherId", "Unknown card", 0, "coolshit"),
            repository.loadKeyCard("someOtherId")
        )
    }

    @Test(expected = NoSuchElementException::class)
    fun loadKeyCardNotFound() = runBlockingTest {
        repository.loadKeyCard("someOtherId")
    }

    @Test
    fun initCardUninitialized() = runBlockingTest {
        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.InitParams::class.java)
        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Uninitialized)

        repository.initCard(manager, params)

        then(manager).should().start()
        then(manager).should().init(params)
        then(manager).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initCardInitialized() = runBlockingTest {
        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.InitParams::class.java)
        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Initialized("testid"))

        try {
            repository.initCard(manager, params)
            assertTrue("Call should throw an exception",false)
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
            assertEquals("Card already initialized", e.message)
            // expected
        }

        then(manager).should().start()
        then(manager).shouldHaveNoMoreInteractions()
    }

    @Test
    fun pairCardNewPairing() = runBlockingTest {
        given(encryptionManagerMock.encrypt(MockUtils.any())).willAnswer {
            val data = (it.arguments.first() as ByteArray)
            EncryptionManager.CryptoData(data, byteArrayOf(0))
        }
        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.PairingParams::class.java)
        val label = "My;cool;card"
        val index = 9517538246L
        val hash = Sha3Utils.keccak("Gnosis".toByteArray())

        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Initialized("myNewCard"))
        given(manager.pair(MockUtils.any<CardRepository.CardManager.PairingParams>())).willReturn("coolshit")

        val key = KeyPair.fromPrivate(Sha3Utils.keccak("saferulez".toByteArray()))
        val address = Solidity.Address(key.address.asBigInteger())
        val signature = key.sign(hash)
        given(manager.sign(MockUtils.any(), anyString())).willReturn(address to signature)

        assertEquals(address, repository.pairCard(manager, params, label, index))

        val storedCardInfo = testPreferences.getString("myNewCard", null)
        assertTrue(storedCardInfo!!.startsWith("636f6f6c73686974####00;My cool card;"))

        then(manager).should().start()
        then(manager).should().pair(params)
        then(manager).should().unlock(params)
        then(manager).should().setupCrypto()
        then(manager).should().sign(hash, "m/44'/60'/0'/0/9517538246")
        then(manager).shouldHaveNoMoreInteractions()
    }

    @Test
    fun pairCardExistingPairing() = runBlockingTest {
        given(encryptionManagerMock.decrypt(MockUtils.any())).willAnswer {
            val cd = (it.arguments.first() as EncryptionManager.CryptoData)
            cd.data
        }
        testPreferences.putString("someOtherId", "coolshit".toByteArray().toHex() + "####1b")

        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.PairingParams::class.java)
        val label = "My;cool;card"
        val index = 9517538246L
        val hash = Sha3Utils.keccak("Gnosis".toByteArray())

        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Initialized("someOtherId"))

        val key = KeyPair.fromPrivate(Sha3Utils.keccak("saferulez".toByteArray()))
        val address = Solidity.Address(key.address.asBigInteger())
        val signature = key.sign(hash)
        given(manager.sign(MockUtils.any(), anyString())).willReturn(address to signature)

        assertEquals(address, repository.pairCard(manager, params, label, index))

        then(manager).should().start()
        then(manager).should().pair("coolshit")
        then(manager).should().unlock(params)
        then(manager).should().setupCrypto()
        then(manager).should().sign(hash, "m/44'/60'/0'/0/9517538246")
        then(manager).shouldHaveNoMoreInteractions()
    }

    @Test
    fun pairCardInvalidRecoverer() = runBlockingTest {
        given(encryptionManagerMock.decrypt(MockUtils.any())).willAnswer {
            val cd = (it.arguments.first() as EncryptionManager.CryptoData)
            cd.data
        }
        testPreferences.putString("someOtherId", "coolshit".toByteArray().toHex() + "####1b")

        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.PairingParams::class.java)
        val label = "My;cool;card"
        val index = 9517538246L
        val hash = Sha3Utils.keccak("Gnosis".toByteArray())

        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Initialized("someOtherId"))
        given(manager.status()).willReturn(CardRepository.CardManager.CardStatus.Initialized("someOtherId"))
        given(manager.clear()).willReturn(true)

        val key = KeyPair.fromPrivate(Sha3Utils.keccak("saferulez".toByteArray()))
        val address = Solidity.Address(key.address.asBigInteger())
        val signature = ECDSASignature(
            "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexAsBigInteger(),
            "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexAsBigInteger()
        ).apply { v = 27 }
        given(manager.sign(MockUtils.any(), anyString())).willReturn(address to signature)

        try {
            repository.pairCard(manager, params, label, index)
            assertTrue("Call should throw an exception",false)
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
            assertEquals("Illegal card address", e.message)
            // expected
        }

        assertNull(testPreferences.getString("someOtherId", null))

        then(manager).should().start()
        then(manager).should().pair("coolshit")
        then(manager).should().unlock(params)
        then(manager).should().setupCrypto()
        then(manager).should().sign(hash, "m/44'/60'/0'/0/9517538246")
        then(manager).should().clear()
        then(manager).should().status()
        then(manager).shouldHaveNoMoreInteractions()
    }

    @Test
    fun pairCardInvalidSignature() = runBlockingTest {
        given(encryptionManagerMock.decrypt(MockUtils.any())).willAnswer {
            val cd = (it.arguments.first() as EncryptionManager.CryptoData)
            cd.data
        }
        testPreferences.putString("someOtherId", "coolshit".toByteArray().toHex() + "####1b")

        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.PairingParams::class.java)
        val label = "My;cool;card"
        val index = 9517538246L
        val hash = Sha3Utils.keccak("Gnosis".toByteArray())

        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Initialized("someOtherId"))
        given(manager.clear()).willReturn(false)

        val key = KeyPair.fromPrivate(Sha3Utils.keccak("saferulez".toByteArray()))
        val address = Solidity.Address(key.address.asBigInteger())
        val signature = ECDSASignature(
            "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexAsBigInteger(),
            "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexAsBigInteger()
        ).apply { v = 35 }
        given(manager.sign(MockUtils.any(), anyString())).willReturn(address to signature)

        try {
            repository.pairCard(manager, params, label, index)
            assertTrue("Call should throw an exception",false)
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
            assertEquals("Could not recover signature", e.message)
            // expected
        }

        assertEquals("coolshit".toByteArray().toHex() + "####1b", testPreferences.getString("someOtherId", null))

        then(manager).should().start()
        then(manager).should().pair("coolshit")
        then(manager).should().unlock(params)
        then(manager).should().setupCrypto()
        then(manager).should().sign(hash, "m/44'/60'/0'/0/9517538246")
        then(manager).should().clear()
        then(manager).shouldHaveNoMoreInteractions()
    }

    @Test
    fun pairCardUninitialized() = runBlockingTest {
        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.PairingParams::class.java)
        val label = "My;cool;card"
        val index = 9517538246L

        testPreferences.putString("someOtherId", "coolshit".toByteArray().toHex() + "####1b")

        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Uninitialized)
        given(manager.clear()).willReturn(false)

        try {
            repository.pairCard(manager, params, label, index)
            assertTrue("Call should throw an exception",false)
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
            assertEquals("Card not initialized", e.message)
            // expected
        }

        assertEquals("coolshit".toByteArray().toHex() + "####1b", testPreferences.getString("someOtherId", null))

        then(manager).should().start()
        then(manager).should().clear()
        then(manager).shouldHaveNoMoreInteractions()
    }

    @Test
    fun signWithCardInitialized() = runBlockingTest {
        given(encryptionManagerMock.decrypt(MockUtils.any())).willAnswer {
            val cd = (it.arguments.first() as EncryptionManager.CryptoData)
            cd.data
        }
        testPreferences.putString("someOtherId", "coolshit".toByteArray().toHex() + "####1b")

        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.UnlockParams::class.java)
        val hash = "9b8bc77908c0b0ebe93e897e43f594b811f5d7130d86a5708403ddb417dc111b".hexToByteArray()
        val index = 9517538264L

        val expected = "0x42".asEthereumAddress()!! to ECDSASignature(BigInteger.ONE, BigInteger.TEN)
        given(manager.sign(MockUtils.any(), anyString())).willReturn(expected)
        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Initialized("someOtherId"))

        assertEquals(expected, repository.signWithCard(manager, params, hash, index))

        then(manager).should().start()
        then(manager).should().pair("coolshit")
        then(manager).should().unlock(params)
        then(manager).should().sign(hash, "m/44'/60'/0'/0/9517538264")
        then(manager).shouldHaveNoMoreInteractions()

    }

    @Test
    fun signWithCardInitializedUnknownCard() = runBlockingTest {

        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.UnlockParams::class.java)
        val hash = "9b8bc77908c0b0ebe93e897e43f594b811f5d7130d86a5708403ddb417dc111b".hexToByteArray()
        val index = 9517538264L

        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Initialized("someOtherId"))

        try {repository.signWithCard(manager, params, hash, index)
            assertTrue("Call should throw an exception",false)
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
            assertEquals("Unknown card", e.message)
            // expected
        }

        then(manager).should().start()
        then(manager).shouldHaveNoMoreInteractions()
    }

    @Test
    fun signWithCardUninitialized() = runBlockingTest {
        val manager = mock(CardRepository.CardManager::class.java)
        val params = mock(CardRepository.CardManager.UnlockParams::class.java)
        val hash = "9b8bc77908c0b0ebe93e897e43f594b811f5d7130d86a5708403ddb417dc111b".hexToByteArray()
        val index = 9517538264L

        given(manager.start()).willReturn(CardRepository.CardManager.CardStatus.Uninitialized)

        try {repository.signWithCard(manager, params, hash, index)
            assertTrue("Call should throw an exception",false)
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
            assertEquals("Card not initialized", e.message)
            // expected
        }

        then(manager).should().start()
        then(manager).shouldHaveNoMoreInteractions()
    }
}