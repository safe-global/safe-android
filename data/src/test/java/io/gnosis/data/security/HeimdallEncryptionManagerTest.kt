package io.gnosis.data.security


import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.security.KeyStorage
import pm.gnosis.tests.utils.TestPreferences
import java.security.KeyStore


class HeimdallEncryptionManagerTest {

    private val keyStorage = mockk<KeyStorage>()

    private lateinit var preferences: TestPreferences
    private lateinit var preferencesManager: PreferencesManager

    private lateinit var encryptionManager: HeimdallEncryptionManager

    private lateinit var biometricPasscodeManager: BiometricPasscodeManager

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        preferencesManager = PreferencesManager(application)

        every { keyStorage.store(any()) } returnsArgument 0
        every { keyStorage.retrieve(any()) } returnsArgument 0
        encryptionManager =
            HeimdallEncryptionManager(preferencesManager = preferencesManager, keyStorage = keyStorage, context = application, provider = KeyStore.getDefaultType())
        biometricPasscodeManager = encryptionManager
    }

    @Test
    fun initialized() {
        assertTrue(encryptionManager.initialized())
    }

    @Test
    fun removePassword() {
        preferences.putString(PREF_KEY_PASSCODE_CHECKSUM, "TEST")
        encryptionManager.removePassword()
        assertEquals(null, preferences.getString(PREF_KEY_PASSCODE_CHECKSUM, null))
    }

    @Test
    fun passwordFlow() {

        preferences.remove(PREF_KEY_PASSCODE_CHECKSUM)

        // Setup with "test"
        assertTrue(encryptionManager.setupPassword("test".toByteArray()))
        verify { keyStorage.store(any()) }

        // Check that it is unlocked
        assertTrue(encryptionManager.unlocked())

        // Check that data can be en- and decrypted
        val encryptedData = encryptionManager.encrypt("Hello World".toByteArray())
        assertEquals("Hello World", String(encryptionManager.decrypt(encryptedData)))

        // Check that password cannot be changed if one is already set
        assertFalse(encryptionManager.setupPassword("test2".toByteArray()))

        // Check that password can be changed with old password
        assertTrue(encryptionManager.setupPassword("test2".toByteArray(), "test".toByteArray()))

        // Check that encryption manager can be locked
        encryptionManager.lock()
        assertFalse(encryptionManager.unlocked())

        // Check that encryption manager cannot be unlocked with wrong password
        assertFalse(encryptionManager.unlockWithPassword(("invalid".toByteArray())))

        // Check that encryption manager can be unlocked with old password
        assertTrue(encryptionManager.unlockWithPassword(("test2".toByteArray())))

        // Check that data encrypted with old password can still be decrypted
        assertEquals("Hello World", String(encryptionManager.decrypt(encryptedData)))
    }

    companion object {
        private const val PREF_KEY_PASSCODE_CHECKSUM = "encryption_manager.string.password_checksum"
    }
}
