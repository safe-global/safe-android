package io.gnosis.safe.utils

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexToByteArray

class OwnerKeyHandlerTest {

    private lateinit var preferences: TestPreferences
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var ownerKeyHandler: OwnerKeyHandler

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        preferencesManager = PreferencesManager(application)

        encryptionManager = mockk<EncryptionManager>(relaxed = true).apply {
            every {
                initialized()
            } returns true

            every {
                unlocked()
            } returns true

            every {
                unlockWithPassword(OwnerKeyHandler.HARDCODED_PASSWORD.toByteArray())
            } returns true

            every {
//                decrypt(EncryptionManager.CryptoData("0xbe5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(), "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray()))
                decrypt(any())
            } returns "00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexToByteArray()

            every {
                encrypt("00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexToByteArray())
            } returns EncryptionManager.CryptoData("be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(), "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray())
        }
        ownerKeyHandler = OwnerKeyHandler(encryptionManager, preferencesManager)
    }

    @Test
    fun `storeKey (with specific key) should store encrypted key and iv in preferences`() {

        ownerKeyHandler.storeKey("0xda18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexAsBigInteger())

        // Assert value in preferences
        Assert.assertEquals("Wrong result", "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249", preferences.getString(OwnerKeyHandler.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, ""))
        Assert.assertEquals("Wrong iv", "0d606ea9dba4abfee35e2119babd9d9e", preferences.getString(OwnerKeyHandler.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, ""))

        // verifyCalls ?
//        verify {
//            // initialized
//            // unlockWithPassword
//            // encrypt
//        }
    }

    @Test
    fun `retrieveKey (___) should ___`() {
        preferences.putString(OwnerKeyHandler.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249")
        preferences.putString(OwnerKeyHandler.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "0d606ea9dba4abfee35e2119babd9d9e")

        val result = ownerKeyHandler.retrieveKey()

        // Assert value in preferences
        Assert.assertEquals("Wrong result", "00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexAsBigInteger(), result)

        //verifyCalls
//        verify {
//            // initialized
//            // unlockWithPassword
//            // decrypt
//        }
    }

    @Test
    fun storeOwnerAddress() {
    }

    @Test
    fun retrieveOwnerAddress() {
    }
}
