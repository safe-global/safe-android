package io.gnosis.data.repositories

import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.impls.AesEncryptionManager
import pm.gnosis.utils.*
import java.math.BigInteger

@Deprecated("use CredentialsRepository")
interface OwnerCredentialsRepository {
    fun storeCredentials(ownerCredentials: OwnerCredentials)
    fun retrieveCredentials(): OwnerCredentials?
    fun removeCredentials()
   // fun hasCredentials(): Boolean
}

@Deprecated("use CredentialsRepository")
data class OwnerCredentials(
    val address: Solidity.Address,
    val key: BigInteger
)

@Deprecated("use CredentialsRepository")
class OwnerCredentialsVault(
    private val encryptionManager: AesEncryptionManager,
    private val preferencesManager: PreferencesManager
) : OwnerCredentialsRepository {

    init {
        initialize()
    }

    override fun storeCredentials(ownerCredentials: OwnerCredentials) {
        storeKey(ownerCredentials.key)
        storeAddress(ownerCredentials.address)
    }

    override fun retrieveCredentials(): OwnerCredentials? {

//        if (!hasCredentials())
//            return null

        val key = retrieveKey()!!
        val address = retrieveAddress()!!
        return OwnerCredentials(address, key)
    }

    override fun removeCredentials() {
        removeAddress()
        removeKey()
    }

//    override fun hasCredentials(): Boolean {
//        return hasAddress() && hasKey()
//    }

    private fun initialize() {
        if (!encryptionManager.initialized()) {
            val result = encryptionManager.setupPassword(HARDCODED_PASSWORD.toByteArray(), HARDCODED_PASSWORD.toByteArray())
            if (!result) {
                // TODO this needs to be handled better
                throw RuntimeException("EncryptionManger init failed")
            }
        }
    }

    private fun resetPasswordAndUnlock() {
        encryptionManager.removePassword()
        val result = encryptionManager.setupPassword(HARDCODED_PASSWORD.toByteArray())
        if (!result) {
            // This should not happen
            throw RuntimeException("EncryptionManger init failed")
        }
        encryptionManager.unlockWithPassword(HARDCODED_PASSWORD.toByteArray())
    }

    private fun storeAddress(address: Solidity.Address?) {
        if (address == null) {
            preferencesManager.prefs.edit { remove(PREF_KEY_OWNER_ADDRESS) }
        } else {
            preferencesManager.prefs.edit { putString(PREF_KEY_OWNER_ADDRESS, address.asEthereumAddressString()) }
        }
    }

    private fun storeKey(key: BigInteger?) {
        val success = encryptionManager.unlockWithPassword(HARDCODED_PASSWORD.toByteArray())
        if (!success) {
            resetPasswordAndUnlock()
        }
        val keyCryptoData = key?.let { encryptionManager.encrypt(it.toByteArray()) }

        preferencesManager.prefs.edit { putString(PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, keyCryptoData?.data?.toHexString() ?: "") }
        preferencesManager.prefs.edit { putString(PREF_KEY_ENCRYPTED_OWNER_KEY_IV, keyCryptoData?.iv?.toHexString() ?: "") }
    }

    private fun retrieveAddress(): Solidity.Address? =
        preferencesManager.prefs.getString(PREF_KEY_OWNER_ADDRESS, null)?.asEthereumAddress()

    private fun retrieveKey(): BigInteger? {
        val success = encryptionManager.unlockWithPassword(HARDCODED_PASSWORD.toByteArray())
        if (!success) {
            return null
        }
        val encrypted = preferencesManager.prefs.getString(PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE, "")!!.hexToByteArray()
        val iv = preferencesManager.prefs.getString(PREF_KEY_ENCRYPTED_OWNER_KEY_IV, "")!!.hexToByteArray()
        if (encrypted.isEmpty() || iv.isEmpty()) {
            return null
        }
        val decrypted = encryptionManager.decrypt(EncryptionManager.CryptoData(encrypted, iv))

        return decrypted.asBigInteger()
    }

    private fun removeAddress() = storeAddress(null)

    private fun removeKey() = storeKey(null)

    private fun hasAddress(): Boolean = retrieveAddress() != null

    private fun hasKey(): Boolean = retrieveKey() != null

    companion object {
        const val PREF_KEY_OWNER_ADDRESS = "owner_key_handler.string.owner.address"
        const val PREF_KEY_ENCRYPTED_OWNER_KEY_VALUE = "encryption_manager.string.encrypted.value"
        const val PREF_KEY_ENCRYPTED_OWNER_KEY_IV = "encryption_manager.string.encrypted.iv"

        // EncryptionManager needs a password to encrypt the owner key (This can be updated to a user provided password in the future)
        const val HARDCODED_PASSWORD = "Hardcoded Password"
    }
}
