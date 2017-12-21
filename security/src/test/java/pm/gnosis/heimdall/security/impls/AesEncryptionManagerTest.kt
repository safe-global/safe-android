package pm.gnosis.heimdall.security.impls

import android.app.Application
import android.security.keystore.KeyProperties
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.spongycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.security.AuthenticationResultSuccess
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import java.security.AlgorithmParameters
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

@RunWith(MockitoJUnitRunner::class)
class AesEncryptionManagerTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var fingerprintHelperMock: DefaultFingerprintHelper

    private val preferences = TestPreferences()

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var manager: AesEncryptionManager

    @Before
    fun setup() {
        given(application.getSharedPreferences(anyString(), anyInt())).willReturn(preferences)
        preferencesManager = PreferencesManager(application)
        manager = AesEncryptionManager(application, preferencesManager, fingerprintHelperMock)
    }

    @Test
    fun initialized() {
        preferences.remove(PREF_KEY_PASSWORD_ENCRYPTED_APP_KEY)
        val uninitializedObserver = TestObserver<Boolean>()
        manager.initialized().subscribe(uninitializedObserver)
        uninitializedObserver.assertNoErrors().assertValue(false)

        preferences.putString(PREF_KEY_PASSWORD_ENCRYPTED_APP_KEY, "TEST")
        val initializedObserver = TestObserver<Boolean>()
        manager.initialized().subscribe(initializedObserver)
        initializedObserver.assertNoErrors().assertValue(true)
    }

    @Test
    fun passwordFlow() {
        val setupObserver = TestObserver<Boolean>()
        preferences.remove(PREF_KEY_PASSWORD_CHECKSUM)

        // Setup with "test"
        manager.setupPassword("test".toByteArray()).subscribe(setupObserver)
        setupObserver.assertNoErrors().assertValue(true)

        // Check that it is unlocked
        val unlockedObserver = TestObserver<Boolean>()
        manager.unlocked().subscribe(unlockedObserver)
        unlockedObserver.assertNoErrors().assertValue(true)

        // Check that data can be en- and decrypted
        val encryptedData = manager.encrypt("Hello World".toByteArray())
        assertEquals("Hello World", String(manager.decrypt(encryptedData)))

        // Check that password cannot be changed if one is already set
        val invalidChangeObserver = TestObserver<Boolean>()
        manager.setupPassword("test2".toByteArray()).subscribe(invalidChangeObserver)
        invalidChangeObserver.assertNoErrors().assertValue(false)

        // Check that password can be changed with old password
        val validChangeObserver = TestObserver<Boolean>()
        manager.setupPassword("test2".toByteArray(), "test".toByteArray()).subscribe(validChangeObserver)
        validChangeObserver.assertNoErrors().assertValue(true)

        // Check that device can be locked
        manager.lock()
        val lockedObserver = TestObserver<Boolean>()
        manager.unlocked().subscribe(lockedObserver)
        lockedObserver.assertNoErrors().assertValue(false)

        // Check that device cannot be unlocked with wrong password
        val wrongPasswordObserver = TestObserver<Boolean>()
        manager.unlockWithPassword(("invalid".toByteArray())).subscribe(wrongPasswordObserver)
        wrongPasswordObserver.assertNoErrors().assertValue(false)

        // Check that device can be unlocked with new password
        val unlockObserver = TestObserver<Boolean>()
        manager.unlockWithPassword(("test2".toByteArray())).subscribe(unlockObserver)
        unlockObserver.assertNoErrors().assertValue(true)

        // Check that data encrypted with old password can still be decrypted
        assertEquals("Hello World", String(manager.decrypt(encryptedData)))
    }

    @Test
    fun testObserveFingerprintForSetupPasswordSet() {
        val cipherMock = mock(Cipher::class.java)
        val algorithmParametersMock = mock(AlgorithmParameters::class.java)
        val ivParameterSpecMock = mock(IvParameterSpec::class.java)
        val authenticationResult = AuthenticationResultSuccess(cipherMock)
        val testObserver = TestObserver.create<Boolean>()

        //TODO get valid cipher else encryption will fail
        given(fingerprintHelperMock.authenticate()).willReturn(Observable.just(authenticationResult))
        given(cipherMock.doFinal(MockUtils.any())).willReturn(byteArrayOf(0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf))
        given(cipherMock.parameters).willReturn(algorithmParametersMock)
        given(algorithmParametersMock.getParameterSpec(IvParameterSpec::class.java)).willReturn(ivParameterSpecMock)
        given(ivParameterSpecMock.iv).willReturn(byteArrayOf(0x0))
        given(fingerprintHelperMock.authenticate(MockUtils.any())).willReturn(Observable.just(authenticationResult))

        val passwordInput = byteArrayOf(0x0)
        val data = byteArrayOf(0x2)
        manager.setupPassword(passwordInput).subscribe(TestObserver())
        val encryptedData = manager.encrypt(data)
        manager.observeFingerprintForSetup().subscribe(testObserver)
        manager.lock()
        manager.observeFingerprintForUnlock().subscribe(TestObserver())
        val decryptedData = manager.decrypt(encryptedData)
        assertEquals(data, decryptedData)

        then(fingerprintHelperMock).should(times(2)).authenticate()
        then(fingerprintHelperMock).shouldHaveNoMoreInteractions()
        val cryptoDataString = preferences.getString(PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY, null)
        val cryptoDataExpected = EncryptionManager.CryptoData(byteArrayOf(0x1), byteArrayOf(0x0))
        assertEquals(cryptoDataExpected.toString(), cryptoDataString)
        testObserver.assertResult(true)
    }

    @Test
    fun testObserveFingerprintForSetupPasswordNotSet() {
        val cipherMock = mock(Cipher::class.java)
        val authenticationResult = AuthenticationResultSuccess(cipherMock)
        val testObserver = TestObserver.create<Boolean>()
        given(fingerprintHelperMock.authenticate()).willReturn(Observable.just(authenticationResult))

        manager.observeFingerprintForSetup().subscribe(testObserver)

        then(fingerprintHelperMock).should().authenticate()
        then(fingerprintHelperMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(false)
    }

    @Test
    fun testObserveFingerprintForSetupFingerprintError() {
        val testObserver = TestObserver<Boolean>()
        val exception = Exception()
        given(fingerprintHelperMock.authenticate()).willReturn(Observable.error(exception))

        manager.observeFingerprintForSetup().subscribe(testObserver)

        then(fingerprintHelperMock).should().authenticate()
        then(fingerprintHelperMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    companion object {
        private const val PREF_KEY_PASSWORD_ENCRYPTED_APP_KEY = "encryption_manager.string.password_encrypted_app_key"
        private const val PREF_KEY_PASSWORD_CHECKSUM = "encryption_manager.string.password_checksum"
        private const val PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY = "encryption_manager.string.fingerprint_encrypted_app_key"
    }
}
