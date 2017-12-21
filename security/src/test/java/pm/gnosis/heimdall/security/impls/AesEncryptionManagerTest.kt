package pm.gnosis.heimdall.security.impls

import android.app.Application
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.stubbing.Answer
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.security.*
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.toHexString
import pm.gnosis.utils.utf8String
import java.security.AlgorithmParameters
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
    private lateinit var fingerprintHelperMock: FingerprintHelper

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
    fun fingerprintFlow() {
        val cipherMock = mock(Cipher::class.java)
        val algorithmParametersMock = mock(AlgorithmParameters::class.java)
        val ivParameterSpecMock = mock(IvParameterSpec::class.java)
        val authenticationResult = AuthenticationResultSuccess(cipherMock)

        given(fingerprintHelperMock.removeKey()).willReturn(Completable.complete())
        given(fingerprintHelperMock.authenticate()).willReturn(Observable.just(authenticationResult))
        given(cipherMock.parameters).willReturn(algorithmParametersMock)
        given(algorithmParametersMock.getParameterSpec(IvParameterSpec::class.java)).willReturn(ivParameterSpecMock)
        given(ivParameterSpecMock.iv).willReturn(byteArrayOf(0x0))
        given(fingerprintHelperMock.authenticate(MockUtils.any())).willReturn(Observable.just(authenticationResult))

        // Setup password to generate app key
        val passwordInput = byteArrayOf(0x0)
        manager.setupPassword(passwordInput).subscribe(TestObserver())

        // Encrypt data
        val data = "Merry Christmas".toByteArray()
        val encryptedData = manager.encrypt(data)

        // Setup encryption mock for fingerprint setup
        val encryptDoFinalAnswer = CachedAnswer<ByteArray, ByteArray>("ENCRYPTED_KEY".toByteArray())
        given(cipherMock.doFinal(MockUtils.any())).will(encryptDoFinalAnswer)

        // Setup fingerprint
        val setupObserver = TestObserver.create<Boolean>()
        manager.observeFingerprintForSetup().subscribe(setupObserver)
        setupObserver.assertResult(true)
        assertNotNull("App key should not be null", encryptDoFinalAnswer.input)
        then(fingerprintHelperMock).should().authenticate()

        // Check that correct data is stored
        val cryptoDataString = preferences.getString(PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY, null)
        val cryptoDataExpected = EncryptionManager.CryptoData(encryptDoFinalAnswer.output, ivParameterSpecMock.iv)
        assertEquals(cryptoDataExpected.toString(), cryptoDataString)

        // Lock device
        manager.lock()

        // Setup encryption mock for fingerprint unlock
        val decryptDoFinalAnswer = CachedAnswer<ByteArray, ByteArray>(encryptDoFinalAnswer.input!!)
        given(cipherMock.doFinal(MockUtils.any())).will(decryptDoFinalAnswer)

        // Unlock with fingerprint
        val unlockObserver = TestObserver.create<FingerprintUnlockResult>()
        manager.observeFingerprintForUnlock().subscribe(unlockObserver)
        unlockObserver.assertSubscribed()
                .assertValue { it is FingerprintUnlockSuccessful }
                .assertNoErrors()
                .assertComplete()
        assertEquals("Encrypted app key should the same that has be returned on setup", encryptDoFinalAnswer.output.toHexString(), decryptDoFinalAnswer.input?.toHexString())
        then(fingerprintHelperMock).should().authenticate(ivParameterSpecMock.iv)

        // Check that decrypted data is correct
        val decryptedData = manager.decrypt(encryptedData)
        assertEquals("Data was not correctly decryted", data.utf8String(), decryptedData.utf8String())

        // Check that the fingerprint can be removed
        val clearObserver = TestObserver<Any>()
        manager.clearFingerprintData().subscribe(clearObserver)
        clearObserver.assertResult()
        then(fingerprintHelperMock).should().removeKey()
        assertNull("Fingerprint encrypted app key should have been removed.", preferences.getString(PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY, null))


        // Check that we don't make unnecessary call to the fingerprint manager
        then(fingerprintHelperMock).shouldHaveNoMoreInteractions()
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

    private class CachedAnswer<I, O>(val output: O) : Answer<O> {
        var input: I? = null

        override fun answer(invocation: InvocationOnMock): O {
            // It should only be set once (the mock call will invoke it too .... )
            input = input ?: invocation.getArgument<I>(0)
            return output
        }
    }

    companion object {
        private const val PREF_KEY_PASSWORD_ENCRYPTED_APP_KEY = "encryption_manager.string.password_encrypted_app_key"
        private const val PREF_KEY_PASSWORD_CHECKSUM = "encryption_manager.string.password_checksum"
        private const val PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY = "encryption_manager.string.fingerprint_encrypted_app_key"
    }
}
