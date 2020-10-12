package io.gnosis.safe.utils

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexToByteArray

class OwnerCredentialsVaultTest {

    private lateinit var preferences: TestPreferences
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var ownerCredentialsVault: OwnerCredentialsRepository

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        preferencesManager = PreferencesManager(application)

        encryptionManager = mockk<EncryptionManager>(relaxed = true).apply {
            every { initialized() } returns true
            every { unlocked() } returns true
            every { unlockWithPassword(OwnerCredentialsVault.HARDCODED_PASSWORD.toByteArray()) } returns true
            every {
                decrypt(
                    EncryptionManager.CryptoData(
                        "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(),
                        "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray()
                    )
                )
            } returns "00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexToByteArray()

            every {
                encrypt("00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexToByteArray())
            } returns EncryptionManager.CryptoData(
                "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(),
                "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray()
            )
        }
        ownerCredentialsVault = OwnerCredentialsVault(encryptionManager, preferencesManager)
    }

    @Test
    fun `storeCredentials (with specific key and address) should store address, encrypted key and iv in preferences`() {
        assertFalse(ownerCredentialsVault.hasCredentials())

        ownerCredentialsVault.storeCredentials(
            OwnerCredentials(
                "0x0000000000000000000000000000000000001234".asEthereumAddress()!!,
                "0xda18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexAsBigInteger()
            )
        )

        assertTrue(ownerCredentialsVault.hasCredentials())
        // Assert value in preferences
        Assert.assertEquals(
            "Wrong result",
            "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249",
            preferences.getString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, "")
        )
        Assert.assertEquals(
            "Wrong iv",
            "0d606ea9dba4abfee35e2119babd9d9e",
            preferences.getString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "")
        )

        // verifyCalls ?
        verify { encryptionManager.initialized() }
        verify { encryptionManager.unlockWithPassword(OwnerCredentialsVault.HARDCODED_PASSWORD.toByteArray()) }
        verify { encryptionManager.encrypt(any()) }

    }

    @Test
    fun `storeCredentials (while not initialized) should store after password setup`() {
        encryptionManager = mockk<EncryptionManager>(relaxed = true).apply {
            every { initialized() } returns false
            every { setupPassword(any(), any()) } returns true
            every { unlockWithPassword(OwnerCredentialsVault.HARDCODED_PASSWORD.toByteArray()) } returns true
            every {
                encrypt("00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexToByteArray())
            } returns EncryptionManager.CryptoData(
                "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(),
                "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray()
            )
            every {
                decrypt(
                    EncryptionManager.CryptoData(
                        "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(),
                        "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray()
                    )
                )
            } returns "00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexToByteArray()
        }
        ownerCredentialsVault = OwnerCredentialsVault(encryptionManager, preferencesManager)

        ownerCredentialsVault.storeCredentials(
            OwnerCredentials(
                "0x0000000000000000000000000000000000001234".asEthereumAddress()!!,
                "0xda18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexAsBigInteger()
            )
        )

        assertTrue(ownerCredentialsVault.hasCredentials())
        // Assert value in preferences
        Assert.assertEquals(
            "Wrong result",
            "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249",
            preferences.getString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, "")
        )
        Assert.assertEquals(
            "Wrong iv",
            "0d606ea9dba4abfee35e2119babd9d9e",
            preferences.getString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "")
        )

        verify { encryptionManager.initialized() }
        verify { encryptionManager.setupPassword(any(), any()) }
        verify { encryptionManager.unlockWithPassword(any()) }

    }


    @Test
    fun `storeCredentials (hardcoded password was wrong) should reset password`() {
        encryptionManager = mockk<EncryptionManager>(relaxed = true).apply {
            every { initialized() } returns false
            every { setupPassword(any(), any()) } returns true
            every { unlockWithPassword(OwnerCredentialsVault.HARDCODED_PASSWORD.toByteArray()) } returns false
            every {
                encrypt("00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexToByteArray())
            } returns EncryptionManager.CryptoData(
                "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(),
                "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray()
            )
            every {
                decrypt(
                    EncryptionManager.CryptoData(
                        "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(),
                        "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray()
                    )
                )
            } returns "00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexToByteArray()
        }
        ownerCredentialsVault = OwnerCredentialsVault(encryptionManager, preferencesManager)

        ownerCredentialsVault.storeCredentials(
            OwnerCredentials(
                "0x0000000000000000000000000000000000001234".asEthereumAddress()!!,
                "0xda18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexAsBigInteger()
            )
        )

        // Assert value in preferences
        Assert.assertEquals(
            "Wrong result",
            "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249",
            preferences.getString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, "")
        )
        Assert.assertEquals(
            "Wrong iv",
            "0d606ea9dba4abfee35e2119babd9d9e",
            preferences.getString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "")
        )
        Assert.assertEquals(
            "Wrong address",
            "0x0000000000000000000000000000000000001234",
            preferences.getString(OwnerCredentialsVault.PREF_KEY_OWNER_ADDRESS, "")
        )

        // verifyCall
        verify { encryptionManager.removePassword() }
    }

    @Test
    fun `retrieveCredentials should return decrypted key and address`() {
        preferences.putString(
            OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE,
            "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249"
        )
        preferences.putString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "0d606ea9dba4abfee35e2119babd9d9e")
        preferences.putString(OwnerCredentialsVault.PREF_KEY_OWNER_ADDRESS, "0x123456")

        val result = ownerCredentialsVault.retrieveCredentials()

        // Assert values in preferences
        Assert.assertEquals("Wrong result", "00da18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexAsBigInteger(), result?.key)
        Assert.assertEquals("Wrong address", "0x123456".asEthereumAddress(), result?.address)

        verify { encryptionManager.initialized() }
        verify { encryptionManager.unlockWithPassword(OwnerCredentialsVault.HARDCODED_PASSWORD.toByteArray()) }
        verify {
            encryptionManager.decrypt(
                EncryptionManager.CryptoData(
                    "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249".hexToByteArray(),
                    "0d606ea9dba4abfee35e2119babd9d9e".hexToByteArray()
                )
            )
        }

    }

    @Test
    fun `removeCredentials should remove credentials`() {
        preferences.putString(
            OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE,
            "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249"
        )
        preferences.putString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "0d606ea9dba4abfee35e2119babd9d9e")
        preferences.putString(OwnerCredentialsVault.PREF_KEY_OWNER_ADDRESS, "0x123456")
        assertTrue(ownerCredentialsVault.hasCredentials())

        ownerCredentialsVault.removeCredentials()

        assertFalse(ownerCredentialsVault.hasCredentials())
    }

    @Test
    fun `hasCredentials (key, iv and address) should return true`() {
        preferences.putString(
            OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE,
            "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249"
        )
        preferences.putString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "0d606ea9dba4abfee35e2119babd9d9e")
        preferences.putString(OwnerCredentialsVault.PREF_KEY_OWNER_ADDRESS, "0x123456")
        assertTrue(ownerCredentialsVault.hasCredentials())

        ownerCredentialsVault.removeCredentials()

        assertFalse(ownerCredentialsVault.hasCredentials())
    }

    @Test
    fun `hasCredentials (no address) should return false`() {
        preferences.putString(
            OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE,
            "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249"
        )
        preferences.putString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "0d606ea9dba4abfee35e2119babd9d9e")

        ownerCredentialsVault.removeCredentials()

        assertFalse(ownerCredentialsVault.hasCredentials())
    }

    @Test
    fun `hasCredentials (no iv) should return false`() {
        preferences.putString(
            OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE,
            "be5173c1f20949055c5076ff4805c6f74cb1b6849631fce61eeb749ae55a7004b2b266631dc16884a3158d5a3f5fd249"
        )
        preferences.putString(OwnerCredentialsVault.PREF_KEY_OWNER_ADDRESS, "0x123456")

        assertFalse(ownerCredentialsVault.hasCredentials())
    }

    @Test
    fun `hasCredentials (key iv) should return false`() {
        preferences.putString(OwnerCredentialsVault.PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "0d606ea9dba4abfee35e2119babd9d9e")
        preferences.putString(OwnerCredentialsVault.PREF_KEY_OWNER_ADDRESS, "0x123456")

        assertFalse(ownerCredentialsVault.hasCredentials())
    }
}


