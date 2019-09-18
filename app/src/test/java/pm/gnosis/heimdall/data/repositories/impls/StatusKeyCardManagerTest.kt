package pm.gnosis.heimdall.data.repositories.impls

import im.status.keycard.applet.*
import im.status.keycard.io.APDUResponse
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.heimdall.data.repositories.CardRepository
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexToByteArray

@RunWith(MockitoJUnitRunner::class)
class StatusKeyCardManagerTest {

    @Mock
    lateinit var cmdSetMock: KeycardCommandSet

    @Mock
    lateinit var responseMock: APDUResponse

    lateinit var manager: StatusKeyCardManager

    @Before
    fun setUp() {
        manager = StatusKeyCardManager(cmdSetMock)
    }

    @Test
    fun statusUninitialized() {
        assertNull(manager.status())

        given(cmdSetMock.select()).willReturn(responseMock)
        given(responseMock.checkOK()).willReturn(responseMock)
        given(responseMock.data).willReturn(uninitializedCard)

        val status = manager.start()
        assertTrue(status is CardRepository.CardManager.CardStatus.Uninitialized)
        assertEquals(status, manager.status())
    }

    @Test
    fun statusInitialized() {
        assertNull(manager.status())

        given(cmdSetMock.select()).willReturn(responseMock)
        given(responseMock.checkOK()).willReturn(responseMock)
        given(responseMock.data).willReturn(initializedCard)

        val status = manager.start()
        check(status is CardRepository.CardManager.CardStatus.Initialized)
        assertEquals(status.id, "2a")
        assertEquals(status, manager.status())

        then(cmdSetMock).should().select()
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).should().checkOK()
        then(responseMock).should().data
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test(expected = IllegalArgumentException::class)
    fun initInvalidParams() {
        manager.init(object : CardRepository.CardManager.InitParams {})
    }

    @Test
    fun init() {
        given(cmdSetMock.init(anyString(), anyString(), anyString())).willReturn(responseMock)

        manager.init(StatusKeyCardManager.InitParams("123456", "1234567890", "coolshit"))

        then(cmdSetMock).should().init("123456", "1234567890", "coolshit")
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).should().checkOK()
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test(expected = IllegalArgumentException::class)
    fun unlockInvalidParams() {
        manager.unlock(object : CardRepository.CardManager.PairingParams {})
    }

    @Test
    fun unlockUnlockParams() {
        given(cmdSetMock.verifyPIN(anyString())).willReturn(responseMock)

        manager.unlock(StatusKeyCardManager.UnlockParams("123456"))

        then(cmdSetMock).should().verifyPIN("123456")
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).should().checkAuthOK()
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun unlockPairingParams() {
        given(cmdSetMock.verifyPIN(anyString())).willReturn(responseMock)

        manager.unlock(StatusKeyCardManager.PairingParams("12345", "coolshit"))

        then(cmdSetMock).should().verifyPIN("12345")
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).should().checkAuthOK()
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test(expected = IllegalArgumentException::class)
    fun pairParamsInvalid() {
        manager.pair(object : CardRepository.CardManager.PairingParams {})
    }

    @Test(expected = IllegalStateException::class)
    fun pairParamsNotStarted() {
        manager.pair(StatusKeyCardManager.PairingParams("12345", "coolshit"))
    }

    @Test
    fun pairParamsInsecure() {
        startManagerAndVerify(false)

        assertEquals("", manager.pair(StatusKeyCardManager.PairingParams("12345", "coolshit")))
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).shouldHaveNoMoreInteractions()

    }

    @Test
    fun pairParamsSecure() {
        startManagerAndVerify(true)

        val pairing = Pairing(byteArrayOf(0, 1, 2, 3, 4, 5))
        given(cmdSetMock.pairing).willReturn(pairing)
        assertEquals(pairing.toBase64(), manager.pair(StatusKeyCardManager.PairingParams("12345", "coolshit")))
        then(cmdSetMock).should().autoPair("coolshit")
        then(cmdSetMock).should().autoOpenSecureChannel()
        then(cmdSetMock).should().pairing
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).shouldHaveNoMoreInteractions()

    }

    @Test(expected = IllegalStateException::class)
    fun pairSessionNotStarted() {
        manager.pair("")
    }

    @Test
    fun pairSessionInsecure() {
        startManagerAndVerify(false)

        manager.pair("coolshit")

        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun pairSessionSecure() {
        startManagerAndVerify(true)

        manager.pair("coolshit")

        then(cmdSetMock).should().pairing = MockUtils.any()
        then(cmdSetMock).should().autoOpenSecureChannel()
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test(expected = IllegalStateException::class)
    fun setupCryptoNotStarted() {
        manager.setupCrypto()
    }

    @Test
    fun setupCryptoNoKey() {
        startManagerAndVerify(true, byteArrayOf(0))

        manager.setupCrypto()

        then(cmdSetMock).should().generateKey()
        then(cmdSetMock).should().setNDEF(byteArrayOf())
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun setupCryptoNoKeyNoMngt() {
        startManagerAndVerify(true, byteArrayOf(0, ApplicationInfo.TLV_CAPABILITIES, 1, 0))

        manager.setupCrypto()

        then(cmdSetMock).should().setNDEF(byteArrayOf())
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun setupCryptoWithKey() {
        startManagerAndVerify(true, byteArrayOf(1, 13))

        manager.setupCrypto()

        then(cmdSetMock).should().setNDEF(byteArrayOf())
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun sign() {
        val publicKey = "0414bcddd11fc0b4c6202574c82583c2bafc1f0639aec7017fd43a72b9407a1dd9fa99ba615abebb2de3c73c61b41ef0f9fb260f50f59a06d03901ccdaad7674cf".hexToByteArray()
        given(cmdSetMock.deriveKey(anyString())).willReturn(responseMock)
        given(cmdSetMock.sign(MockUtils.any())).willReturn(responseMock)
        given(responseMock.checkOK()).willReturn(responseMock)
        given(responseMock.data).willReturn(
            byteArrayOf(
                RecoverableSignature.TLV_SIGNATURE_TEMPLATE, 32,
                ApplicationInfo.TLV_PUB_KEY, 65
            ) + publicKey + byteArrayOf(
                RecoverableSignature.TLV_ECDSA_TEMPLATE, 64,
                TinyBERTLV.TLV_INT, 32
            ) + "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexToByteArray() + byteArrayOf(
                TinyBERTLV.TLV_INT, 32
            ) + "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexToByteArray()
        )

        val hash = "9b8bc77908c0b0ebe93e897e43f594b811f5d7130d86a5708403ddb417dc111b".hexToByteArray()
        val expectedAddress = "0xa5056c8efadb5d6a1a6eb0176615692b6e648313".asEthereumAddress()
        val expectedSignature = ECDSASignature(
            "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexAsBigInteger(),
            "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexAsBigInteger()
        ).apply { v = 27 }

        val (actualAddress, actualSignature) = manager.sign(hash, "ThisWouldBeAnInvalidPath")
        assertEquals(expectedAddress, actualAddress)
        assertEquals(expectedSignature.r, actualSignature.r)
        assertEquals(expectedSignature.s, actualSignature.s)
        assertEquals(expectedSignature.v, actualSignature.v)
    }

    @Test
    fun clearNoPairing() {
        manager.clear()
        then(cmdSetMock).should().pairing
        then(cmdSetMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun clear() {
        given(cmdSetMock.pairing).willReturn(Pairing("coolshit"))
        manager.clear()
        then(cmdSetMock).should().pairing
        then(cmdSetMock).should().autoUnpair()
        then(cmdSetMock).shouldHaveNoMoreInteractions()
    }

    private fun startManagerAndVerify(secure: Boolean, keyBytes: ByteArray = byteArrayOf(1, 13)) {
        given(cmdSetMock.select()).willReturn(responseMock)
        given(responseMock.checkOK()).willReturn(responseMock)
        given(responseMock.data).willReturn(if (secure) (baseInitializedCard + keyBytes) else uninitializedCard)

        manager.start()

        then(cmdSetMock).should().select()
        then(cmdSetMock).shouldHaveNoMoreInteractions()
        then(responseMock).should().checkOK()
        then(responseMock).should().data
        then(responseMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val uninitializedCard = byteArrayOf(
            ApplicationInfo.TLV_PUB_KEY,
            0.toByte(),
            0.toByte()
        )
        private val baseInitializedCard = byteArrayOf(
            ApplicationInfo.TLV_APPLICATION_INFO_TEMPLATE,
            42.toByte(),
            ApplicationInfo.TLV_UID,
            1.toByte(),
            42.toByte(),
            ApplicationInfo.TLV_PUB_KEY,
            1.toByte(),
            1.toByte(),
            TinyBERTLV.TLV_INT,
            1.toByte(),
            5.toByte(),
            TinyBERTLV.TLV_INT,
            1.toByte(),
            3.toByte(),
            ApplicationInfo.TLV_KEY_UID
        )
        private val initializedCard = baseInitializedCard + byteArrayOf(
            1.toByte(),
            13.toByte()
        )
    }
}