package io.gnosis.data.repositories

import io.gnosis.data.db.daos.OwnerDao
import io.gnosis.data.models.Owner
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.data.utils.toSignatureString
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import java.math.BigInteger

class CredentialsRepository(
    private val ownerDao: OwnerDao,
    private val encryptionManager: HeimdallEncryptionManager,
    private val preferencesManager: PreferencesManager
) {

    suspend fun saveOwner(
        address: Solidity.Address,
        key: BigInteger,
        name: String? = null
    ) {
        encryptionManager.unlock()
        val owner = Owner(
            address = address,
            name = name,
            type = Owner.Type.LOCALLY_STORED,
            privateKey = EncryptedByteArray.create(encryptionManager, key.toByteArray())
        )
        encryptionManager.lock()
        ownerDao.save(owner)
    }

    suspend fun saveOwner(owner: Owner) {
        ownerDao.save(owner)
    }

    suspend fun removeOwner(owner: Owner) {
        ownerDao.delete(owner)
    }

    fun signWithOwner(owner: Owner, data: ByteArray): String {
        val cryptoData = EncryptionManager.CryptoData.fromString(owner.privateKey.toString())
        encryptionManager.unlock()
        val key = encryptionManager.decrypt(cryptoData)
        encryptionManager.lock()
        val keyPair = KeyPair.fromPrivate(key)
        return keyPair
            .sign(data)
            .toSignatureString()
    }
}

