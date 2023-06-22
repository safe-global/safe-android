package io.gnosis.data.repositories

import io.gnosis.data.db.daos.OwnerDao
import io.gnosis.data.models.Owner
import io.gnosis.data.models.OwnerTypeConverter
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.data.utils.toSignatureString
import kotlinx.coroutines.runBlocking
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.svalinn.security.db.EncryptedString
import pm.gnosis.utils.asBigInteger
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

    fun credentialsUnlocked(): Boolean {
        return encryptionManager.unlocked()
    }

    suspend fun ownerCount(ownerType: Owner.Type? = null): Int {
        return when {
            ownerType == Owner.Type.IMPORTED || ownerType == Owner.Type.GENERATED ||ownerType == Owner.Type.LEDGER_NANO_X ||ownerType == Owner.Type.KEYSTONE -> {
                ownerDao.ownerCountForType(OwnerTypeConverter().toValue(ownerType))
            }
            else -> {
                ownerDao.ownerCount()
            }
        }
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
            type = Owner.Type.IMPORTED,
            privateKey = encryptedKey,
            seedPhrase = null
        )
        ownerDao.save(owner)
    }

    suspend fun saveOwnerGenerated(
        seedPhrase: String,
        address: Solidity.Address,
        key: BigInteger,
        name: String? = null
    ) {
        val encryptedKey = encryptKey(key)
        val encryptedSeedPhrase = encryptSeed(seedPhrase)
        val owner = Owner(
            address = address,
            name = name,
            type = Owner.Type.GENERATED,
            privateKey = encryptedKey,
            seedPhrase = encryptedSeedPhrase
        )
        ownerDao.save(owner)
    }

    suspend fun saveLedgerOwner(
        derivationPathWithIndex: String,
        address: Solidity.Address,
        name: String? = null
    ) {
        val owner = Owner(
            address = address,
            name = name,
            type = Owner.Type.LEDGER_NANO_X,
            keyDerivationPath = derivationPathWithIndex
        )
        ownerDao.save(owner)
    }

    suspend fun saveKeystoneOwner(
        address: Solidity.Address,
        name: String? = null,
        path: String,
        sourceFingerprint: String
    ) {
        val owner = Owner(
            address = address,
            name = name,
            type = Owner.Type.KEYSTONE,
            keyDerivationPath = path,
            sourceFingerprint = sourceFingerprint
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

    fun decryptKey(encryptedKey: EncryptedByteArray): BigInteger {
        val converter = EncryptedByteArray.Converter()
        val cryptoData = EncryptionManager.CryptoData.fromString(converter.toStorage(encryptedKey))
        encryptionManager.unlock()
        val key = encryptionManager.decrypt(cryptoData)
        encryptionManager.lock()
        return key.asBigInteger()
    }

    fun encryptSeed(data: String): EncryptedString {
        encryptionManager.unlock()
        val encryptedData = EncryptedString.create(encryptionManager, data)
        encryptionManager.lock()
        return encryptedData
    }

    fun decryptSeed(encryptedData: EncryptedString): String {
        encryptionManager.unlock()
        val data = encryptedData.value(encryptionManager)
        encryptionManager.lock()
        return data
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

