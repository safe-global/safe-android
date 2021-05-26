package io.gnosis.data.repositories

import io.gnosis.data.db.daos.OwnerDao
import io.gnosis.data.models.Owner
import io.gnosis.data.security.HeimdallEncryptionManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import java.math.BigInteger

class CredentialsRepositoryTest {

    private lateinit var credentialsRepository: CredentialsRepository
    private val ownerDao = mockk<OwnerDao>()
    private val encryptionManager = mockk<HeimdallEncryptionManager>()
    private val ownerCredentialsVault = mockk<OwnerCredentialsVault>()


    @Test
    fun `signWithOwner (owner, data) should have correct order of locking and unlocking app key`() = runBlocking {

        val key = "decrypted".toByteArray()
        val keyEncryptedString = "0x1234${EncryptionManager.CryptoData.SEPARATOR}0x1234"
        val keyCryptoData =  EncryptionManager.CryptoData.fromString(keyEncryptedString)
        val owner = Owner(Solidity.Address(BigInteger.ONE), "owner", Owner.Type.IMPORTED, EncryptedByteArray.Converter().fromStorage(keyEncryptedString))

        coEvery { ownerCredentialsVault.hasCredentials() } returns false
        coEvery { encryptionManager.unlock() } just Runs
        coEvery { encryptionManager.lock() } just Runs
        coEvery { encryptionManager.decrypt(any()) } returns key

        credentialsRepository = CredentialsRepository(ownerDao, encryptionManager, ownerCredentialsVault)
        credentialsRepository.signWithOwner(owner, "data".toByteArray())

        coVerifySequence {
            encryptionManager.unlock()
            encryptionManager.decrypt(keyCryptoData)
            encryptionManager.lock()
        }
    }
}
