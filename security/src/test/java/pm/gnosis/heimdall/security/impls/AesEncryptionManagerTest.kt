package pm.gnosis.heimdall.security.impls

import android.app.Application
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.TestPreferences

@RunWith(MockitoJUnitRunner::class)
class AesEncryptionManagerTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var application: Application

    private val preferences = TestPreferences()

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var manager: AesEncryptionManager

    @Before
    fun setup() {
        given(application.getSharedPreferences(anyString(), anyInt())).willReturn(preferences)
        preferencesManager = PreferencesManager(application)
        manager = AesEncryptionManager(application, preferencesManager)
    }

    @Test
    fun initialized() {
        preferences.remove(PREF_KEY_APP_KEY)
        val uninitializedObserver = TestObserver<Boolean>()
        manager.initialized().subscribe(uninitializedObserver)
        uninitializedObserver.assertNoErrors().assertValue(false)

        preferences.putString(PREF_KEY_APP_KEY, "TEST")
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


    companion object {
        private const val PREF_KEY_APP_KEY = "encryption_manager.string.app_key"
        private const val PREF_KEY_PASSWORD_CHECKSUM = "encryption_manager.string.password_checksum"
    }

}