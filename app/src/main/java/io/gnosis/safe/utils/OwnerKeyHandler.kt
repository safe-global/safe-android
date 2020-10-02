package io.gnosis.safe.utils

import androidx.core.content.edit
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.math.BigInteger

interface PrivateKeyHandler {
    fun storeKey(key: BigInteger)
    fun retrieveKey(): BigInteger
}

interface OwnerAddressHandler {
    fun storeOwnerAddress(address: Solidity.Address)
    fun retrieveOwnerAddress(): Solidity.Address
}

class OwnerKeyHandler(
        private val encryptionManager: EncryptionManager,
        private val preferencesManager: PreferencesManager
) : PrivateKeyHandler, OwnerAddressHandler { // TODO:  should be two classes
    override fun storeKey(key: BigInteger) {

        // 1. Check if keystore is initialized with encryptionManager.initialized()
        isInitialized()
        val result = encryptionManager.unlockWithPassword(HARDCODED_PASSWORD.toByteArray())
        Timber.i("---> unlockWithPassword: result: $result")

        // 2.a) Encrypt key
        val keyCryptoData = encryptionManager.encrypt(key.toByteArray())

        // 2.b) Store encrypted key in preferences
        preferencesManager.prefs.edit { putString(PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, keyCryptoData.data.toHexString()) }
        preferencesManager.prefs.edit { putString(PREF_KEY_ENCRYPTED_OWNER_KEY_IV, keyCryptoData.iv.toHexString()) }

    }

    private fun isInitialized() {
        Timber.i("---> initialized(): result: ${encryptionManager.initialized()}")
        if (!encryptionManager.initialized()) {
            // 1.a) initialize
            val result = encryptionManager.setupPassword(HARDCODED_PASSWORD.toByteArray())
            if (!result) {
                Timber.i("---> setupPassword: result(): $result")

                // TODO this needs to be handled better
                throw RuntimeException("EncryptionManger init failed")
            }
        }
    }

    override fun retrieveKey(): BigInteger {
        isInitialized()

        val result = encryptionManager.unlockWithPassword(HARDCODED_PASSWORD.toByteArray())
        println("---> unlockWithPassword: result: $result")
        Timber.i("---> unlockWithPassword: result: $result")

        val encrypted = preferencesManager.prefs.getString(PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, "")!!.hexToByteArray()
        val iv = preferencesManager.prefs.getString(PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "")!!.hexToByteArray()
        println("---> encrypted: data: ${encrypted.toHexString()} iv: ${iv.toHexString()}")
        Timber.i("---> encrypted: data: ${encrypted.toHexString()} iv: ${iv.toHexString()}")

        val decrypted = encryptionManager.decrypt(EncryptionManager.CryptoData(encrypted, iv))

        println("---> decrypted: ${decrypted.toHexString()}")
        Timber.i("---> decrypted: ${decrypted.toHexString()}")

        // TODO Maybe change type to a more generic ByteArray?
        return decrypted.asBigInteger()
    }

    override fun storeOwnerAddress(address: Solidity.Address) {
        TODO("Not yet implemented")
    }

    override fun retrieveOwnerAddress(): Solidity.Address {
        TODO("Not yet implemented")
    }

    companion object {
        const val PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE = "encryption_manager.string.encrypted.value"
        const val PREF_KEY_ENCRYPTED_OWNER_KEY_IV = "encryption_manager.string.encrypted.iv"
        // EncryptionManager needs a password to encrypt the owner key (This can be updated to a user provided password in the future)
        const val HARDCODED_PASSWORD = "Hardcoded Password"
    }
}
