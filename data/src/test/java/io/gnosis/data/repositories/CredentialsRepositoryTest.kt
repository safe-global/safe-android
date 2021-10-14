package io.gnosis.data.repositories

import io.gnosis.data.db.daos.OwnerDao
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Owner.Type
import io.gnosis.data.security.HeimdallEncryptionManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.svalinn.security.db.EncryptedString
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.asEthereumAddress
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
        val keyCryptoData = EncryptionManager.CryptoData.fromString(keyEncryptedString)
        val owner = Owner(Solidity.Address(BigInteger.ONE), "owner", Type.IMPORTED, EncryptedByteArray.Converter().fromStorage(keyEncryptedString))

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

    @Test
    fun `encryptKey`() = runBlocking {

        val key = "decrypted".toByteArray()
        val keyEncryptedString = "0x1234${EncryptionManager.CryptoData.SEPARATOR}0x1234"
        val keyCryptoData = EncryptionManager.CryptoData.fromString(keyEncryptedString)
        val owner = Owner(Solidity.Address(BigInteger.ONE), "owner", Owner.Type.IMPORTED, EncryptedByteArray.Converter().fromStorage(keyEncryptedString))

        coEvery { ownerCredentialsVault.hasCredentials() } returns false
        coEvery { encryptionManager.unlock() } just Runs
        coEvery { encryptionManager.lock() } just Runs
        coEvery { encryptionManager.encrypt(any()) } returns keyCryptoData

        credentialsRepository = CredentialsRepository(ownerDao, encryptionManager, ownerCredentialsVault)
        credentialsRepository.encryptKey(key.asBigInteger())

        coVerifySequence {
            encryptionManager.unlock()
            encryptionManager.encrypt(key)
            encryptionManager.lock()
        }
    }

    @Test
    fun `decryptKey`() = runBlocking {

        val key = "decrypted".toByteArray()
        val keyEncryptedString = "0x1234${EncryptionManager.CryptoData.SEPARATOR}0x1234"
        val keyCryptoData = EncryptionManager.CryptoData.fromString(keyEncryptedString)
        val owner = Owner(Solidity.Address(BigInteger.ONE), "owner", Type.IMPORTED, EncryptedByteArray.Converter().fromStorage(keyEncryptedString))

        coEvery { ownerCredentialsVault.hasCredentials() } returns false
        coEvery { encryptionManager.unlock() } just Runs
        coEvery { encryptionManager.lock() } just Runs
        coEvery { encryptionManager.decrypt(any()) } returns key

        credentialsRepository = CredentialsRepository(ownerDao, encryptionManager, ownerCredentialsVault)
        credentialsRepository.decryptKey(owner.privateKey!!)

        coVerifySequence {
            encryptionManager.unlock()
            encryptionManager.decrypt(keyCryptoData)
            encryptionManager.lock()
        }
    }

    @Test
    fun `encryptSeed`() = runBlocking {
        val seed = "seed"
        val seedEncryptedString = "0x1234${EncryptionManager.CryptoData.SEPARATOR}0x1234"
        val seedCryptoData = EncryptionManager.CryptoData.fromString(seedEncryptedString)

        coEvery { ownerCredentialsVault.hasCredentials() } returns false
        coEvery { encryptionManager.unlock() } just Runs
        coEvery { encryptionManager.lock() } just Runs
        coEvery { encryptionManager.encrypt(any()) } returns seedCryptoData

        credentialsRepository = CredentialsRepository(ownerDao, encryptionManager, ownerCredentialsVault)
        credentialsRepository.encryptSeed(seed)

        coVerifySequence {
            encryptionManager.unlock()
            encryptionManager.encrypt(seed.toByteArray())
            encryptionManager.lock()
        }
    }

    @Test
    fun `decryptSeed`() = runBlocking {

        val seed = "seed"
        val seedEncryptedString = "0x1234${EncryptionManager.CryptoData.SEPARATOR}0x1234"
        val seedCryptoData = EncryptionManager.CryptoData.fromString(seedEncryptedString)
        val encryptedSeed = EncryptedString.Converter().fromStorage(seedEncryptedString)

        coEvery { ownerCredentialsVault.hasCredentials() } returns false
        coEvery { encryptionManager.unlock() } just Runs
        coEvery { encryptionManager.lock() } just Runs
        coEvery { encryptionManager.decrypt(any()) } returns seed.toByteArray()

        credentialsRepository = CredentialsRepository(ownerDao, encryptionManager, ownerCredentialsVault)
        credentialsRepository.decryptSeed(encryptedSeed)

        coVerifySequence {
            encryptionManager.unlock()
            encryptionManager.decrypt(seedCryptoData)
            encryptionManager.lock()
        }
    }

    @Test
    fun saveLedgerOwner() {
        coEvery { ownerDao.save(any()) } just Runs
        coEvery { ownerCredentialsVault.hasCredentials() } returns false

        credentialsRepository = CredentialsRepository(ownerDao, encryptionManager, ownerCredentialsVault)

        runBlocking { credentialsRepository.saveLedgerOwner("path/1", "0x00".asEthereumAddress()!!, null) }

        coVerifySequence {
            ownerDao.save(Owner(address = "0x00".asEthereumAddress()!!, name = null, keyDerivationPath = "path/1", type = Type.LEDGER_NANO_X))
        }
    }

    @Test
    @Ignore
    fun saveOwner() {
        val seedEncryptedString = "0x1234${EncryptionManager.CryptoData.SEPARATOR}0x1234"
        val seedCryptoData = EncryptionManager.CryptoData.fromString(seedEncryptedString)
        coEvery { ownerDao.save(any()) } just Runs
        coEvery { ownerCredentialsVault.hasCredentials() } returns false
        coEvery { encryptionManager.unlock() } just Runs
        coEvery { encryptionManager.lock() } just Runs
        coEvery { encryptionManager.encrypt(any()) } returns seedCryptoData

        credentialsRepository = CredentialsRepository(ownerDao, encryptionManager, ownerCredentialsVault)

        runBlocking { credentialsRepository.saveOwner(address = "0x00".asEthereumAddress()!!, key = BigInteger.TEN, name = null) }

        coVerify {
            ownerDao.save(
                    Owner(
                            address = "0x00".asEthereumAddress()!!,
                            name = null,
                            keyDerivationPath = null,
                            type = Type.IMPORTED,
                            privateKey = any())
            )
        }
    }

    @Test
    @Ignore
    fun saveOwnerGenerated() {
        val seedEncryptedString = "0x1234${EncryptionManager.CryptoData.SEPARATOR}0x1234"
        val seedCryptoData = EncryptionManager.CryptoData.fromString(seedEncryptedString)
        coEvery { ownerDao.save(any()) } just Runs
        coEvery { ownerCredentialsVault.hasCredentials() } returns false
        coEvery { encryptionManager.unlock() } just Runs
        coEvery { encryptionManager.lock() } just Runs
        coEvery { encryptionManager.encrypt(any()) } returns seedCryptoData
        val encryptedByteArray = EncryptedByteArray.create(encryptionManager, seedCryptoData.data)
        val encryptedString = EncryptedString.create(encryptionManager, seedCryptoData.toString())

        credentialsRepository = CredentialsRepository(ownerDao, encryptionManager, ownerCredentialsVault)

        runBlocking { credentialsRepository.saveOwnerGenerated(seedPhrase = "seed phrase", address = "0x00".asEthereumAddress()!!, key = BigInteger.TEN) }

        coVerify {
            ownerDao.save(
                    Owner(
                            address = "0x00".asEthereumAddress()!!,
                            type = Type.GENERATED,
                            privateKey = encryptedByteArray,
                            seedPhrase = encryptedString
                    )
            )
        }
    }
}
