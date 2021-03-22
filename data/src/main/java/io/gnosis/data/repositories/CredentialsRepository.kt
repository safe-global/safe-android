package io.gnosis.data.repositories

import io.gnosis.data.db.daos.OwnerDao
import io.gnosis.data.models.Owner
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.data.utils.toSignatureString
import kotlinx.coroutines.runBlocking
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import java.math.BigInteger

class CredentialsRepository(
    private val ownerDao: OwnerDao,
    private val encryptionManager: HeimdallEncryptionManager,
    //FIXME: remove after all users migrate to version with db storage for owners
    private val ownerVault: OwnerCredentialsRepository
) {

    init {
        runBlocking {
            if (ownerVault.hasCredentials()) {
                val credentials = ownerVault.retrieveCredentials()!!
                saveOwner(credentials.address, credentials.key)
                ownerVault.removeCredentials()
            }
        }
    }

    suspend fun ownerCount(): Int {
        return ownerDao.ownerCount()
    }

    suspend fun owners(): List<Owner> {
        return ownerDao.loadAll()
    }

    suspend fun owner(ownerAddress: Solidity.Address): Owner? {
        return ownerDao.loadByAddress(ownerAddress)
    }

    suspend fun saveOwner(
        address: Solidity.Address,
        key: BigInteger,
        name: String? = null
    ) {
        val encryptedKey = encryptKey(key)
        val owner = Owner(
            address = address,
            name = name,
            type = Owner.Type.LOCALLY_STORED,
            privateKey = encryptedKey
        )
        ownerDao.save(owner)
    }

    suspend fun saveOwner(owner: Owner) {
        ownerDao.save(owner)
    }

    suspend fun removeOwner(owner: Owner) {
        ownerDao.delete(owner)
    }

    suspend fun removeOwner(ownerAddress: Solidity.Address) {
        ownerDao.deleteByAddress(ownerAddress)
    }

    fun encryptKey(key: BigInteger): EncryptedByteArray {
        encryptionManager.unlock()
        val encryptedKey = EncryptedByteArray.create(encryptionManager, key.toByteArray())
        encryptionManager.lock()
        return encryptedKey
    }

    fun signWithOwner(owner: Owner, data: ByteArray): String {
        val converter = EncryptedByteArray.Converter()
        val cryptoData = EncryptionManager.CryptoData.fromString(converter.toStorage(owner.privateKey!!))
        encryptionManager.unlock()
        val key = encryptionManager.decrypt(cryptoData)
        encryptionManager.lock()
        val keyPair = KeyPair.fromPrivate(key)
        return keyPair
            .sign(data)
            .toSignatureString()
    }
}

