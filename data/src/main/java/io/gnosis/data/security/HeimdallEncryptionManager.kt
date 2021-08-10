package io.gnosis.data.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.SCrypt
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PKCS7Padding
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.KeyStorage
import pm.gnosis.svalinn.security.exceptions.DeviceIsLockedException
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.toHexString
import java.nio.charset.Charset
import java.security.*
import javax.crypto.Cipher

/**
 * @param passcodeIterations Number of iterations the passcode is hashed to prevent brute force attacks.
 *                           Will be disabled when set to 0 else has to be larger than 1, a power of 2 and less than <code>2^128</code>.
 */
class HeimdallEncryptionManager(
    private val preferencesManager: PreferencesManager,
    private val keyStorage: KeyStorage,
    private val passcodeIterations: Int = SCRYPT_ITERATIONS,
    private val context: Context,
    private val provider: String = "AndroidKeyStore"
) : EncryptionManager, BiometricPasscodeManager {

    private val secureRandom = SecureRandom()
    private var key: ByteArray? = null

    // For BiometricPasscodeManager
    private val RSA_KEY_SIZE = 1024
    private val ASYMMETRIC_ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA
    private val ASYMMETRIC_ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_ECB
    private val ASYMMETRIC_ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
    private val moshi = Moshi.Builder().build()

    init {
        if (preferencesManager.prefs.getString(PREF_KEY_ENCRYPTED_APP_KEY, null) == null) {
            preferencesManager.prefs.edit { putString(PREF_KEY_ENCRYPTED_APP_KEY, generateKey().toHexString()) }
        }
    }

    // not needed
    override fun initialized(): Boolean = true

    @Synchronized
    override fun setupPassword(newPasscode: ByteArray, oldPasscode: ByteArray?): Boolean {

        val checksum = preferencesManager.prefs.getString(PREF_KEY_PASSCODE_CHECKSUM, null)
        if (checksum != null) {
            buildPasscodeKeyIfValid(oldPasscode, checksum) ?: return false
        }

        key = decryptAppKey()
        key?.let {
            val passcodeKey = deriveKeyFromPasscode(newPasscode)
            preferencesManager.prefs.edit { putString(PREF_KEY_PASSCODE_CHECKSUM, generateCryptedChecksum(passcodeKey)) }
        }

        return key != null
    }

    // If we don't know the previous passcode and want to start over
    override fun removePassword() {
        preferencesManager.prefs.edit { remove(PREF_KEY_PASSCODE_CHECKSUM) }
    }


    @Synchronized
    override fun unlockWithPassword(passcode: ByteArray): Boolean {
        // If we have no passcode set (no checksum stored, we cannot unlockWithPasscode
        val checksum = preferencesManager.prefs.getString(PREF_KEY_PASSCODE_CHECKSUM, null) ?: return false
        buildPasscodeKeyIfValid(passcode, checksum) ?: return false
        key = decryptAppKey()
        return true
    }

    @Synchronized
    fun unlock() {
        key = decryptAppKey()
    }

    @Synchronized
    override fun unlocked(): Boolean {
        return key != null
    }

    @Synchronized
    override fun lock() {
        key = null
    }

    @Synchronized
    override fun decrypt(data: EncryptionManager.CryptoData): ByteArray {
        val key = this.key?.let {
            nullOnThrow { keyStorage.retrieve(it) } ?: it // Fallback if app was setup before storage existed
        } ?: throw DeviceIsLockedException()

        return decrypt(key, data)
    }

    @Synchronized
    override fun encrypt(data: ByteArray): EncryptionManager.CryptoData {
        val key = this.key?.let {
            nullOnThrow { keyStorage.retrieve(it) } ?: it // Fallback if app was setup before storage existed
        } ?: throw DeviceIsLockedException()

        return encrypt(key, data)
    }

    private fun deriveKeyFromPasscode(passcode: ByteArray): ByteArray =
        // If password iterations is set to 0 we will not use SCrypt
        if (passcodeIterations == 0)
            Sha3Utils.sha3(passcode)
        else
            SCrypt.generate(passcode, Sha3Utils.sha3(passcode), passcodeIterations, SCRYPT_BLOCK_SIZE, SCRYPT_PARALLELIZATION, SCRYPT_KEY_LENGTH)

    private fun generateKey(): ByteArray {
        val randomBytes = ByteArray(32)
        secureRandom.nextBytes(randomBytes)
        return keyStorage.store(randomBytes)
    }

    private fun randomIv(): ByteArray {
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)
        return randomBytes
    }

    private fun decryptAppKey(): ByteArray? {
        return keyStorage.retrieve(preferencesManager.prefs.getString(PREF_KEY_ENCRYPTED_APP_KEY, null)?.hexToByteArray() ?: return null)
    }

    private fun keyChecksum(key: ByteArray) =
        Sha3Utils.sha3String(key).substring(0, 6).toByteArray()

    private fun buildPasscodeKeyIfValid(passcode: ByteArray?, checksum: String): ByteArray? {
        passcode ?: return null
        val hashedKey = deriveKeyFromPasscode(passcode)
        val decryptedChecksum = nullOnThrow { decrypt(hashedKey, EncryptionManager.CryptoData.fromString(checksum)).toHexString() }
        if (keyChecksum(hashedKey).toHexString() == decryptedChecksum) {
            return hashedKey
        }
        return null
    }

    private fun generateCryptedChecksum(key: ByteArray): String {
        return encrypt(key, keyChecksum(key)).toString()
    }

    private fun encrypt(key: ByteArray, data: ByteArray): EncryptionManager.CryptoData {
        return useCipher(true, key, EncryptionManager.CryptoData(data, randomIv()))
    }

    private fun decrypt(key: ByteArray, data: EncryptionManager.CryptoData): ByteArray {
        return useCipher(false, key, data).data
    }

    private fun useCipher(encrypt: Boolean, key: ByteArray, wrapper: EncryptionManager.CryptoData): EncryptionManager.CryptoData {
        val padding = PKCS7Padding()
        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()), padding)
        cipher.reset()

        val keyParam = KeyParameter(key)
        val params = ParametersWithIV(keyParam, wrapper.iv)
        cipher.init(encrypt, params)

        // create a temporary buffer to decode into (it'll include padding)
        val buf = ByteArray(cipher.getOutputSize(wrapper.data.size))
        var len = cipher.processBytes(wrapper.data, 0, wrapper.data.size, buf, 0)
        len += cipher.doFinal(buf, len)

        // remove padding
        val out = ByteArray(len)
        System.arraycopy(buf, 0, out, 0, len)

        return EncryptionManager.CryptoData(out, wrapper.iv)
    }

    companion object {
        private const val SCRYPT_ITERATIONS = 16384
        private const val SCRYPT_BLOCK_SIZE = 8
        private const val SCRYPT_PARALLELIZATION = 1
        private const val SCRYPT_KEY_LENGTH = 32
        private const val PREF_KEY_ENCRYPTED_APP_KEY = "encryption_manager.string.encrypted_app_key"
        private const val PREF_KEY_PASSCODE_CHECKSUM = "encryption_manager.string.password_checksum"
    }

    // For BiometricPasscodeManager
    @RequiresApi(Build.VERSION_CODES.M)
    override fun getInitializedRSACipherForEncryption(keyName: String): Cipher {
        val cipher = getCipher()
        try {
            val publicKey = getOrCreateKey(keyName, true) as PublicKey
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        } catch (e: java.lang.ClassCastException) {
            deleteKey(keyName)
        }
        return cipher
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun getInitializedRSACipherForDecryption(
        keyName: String
    ): Cipher {
        val cipher = getCipher()
        val privateKey = getOrCreateKey(keyName) as PrivateKey
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher
    }

    override fun deleteKey(keyName: String) {
        val keyStore = KeyStore.getInstance(provider)
        keyStore.load(null)
        keyStore.deleteEntry(keyName)
    }

    override fun encryptData(plaintext: String, cipher: Cipher): PasscodeCiphertextWrapper {
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charset.forName("UTF-8")))
        return PasscodeCiphertextWrapper(ciphertext)
    }

    override fun decryptData(ciphertext: ByteArray, cipher: Cipher): String {
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charset.forName("UTF-8"))
    }

    private fun getCipher(): Cipher {
        val transformation = "$ASYMMETRIC_ENCRYPTION_ALGORITHM/$ASYMMETRIC_ENCRYPTION_BLOCK_MODE/$ASYMMETRIC_ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getOrCreateKey(keyName: String, pubKey: Boolean = false): Key {
        // If Secretkey was previously created for that keyName, then grab and return it.
        val keyStore = KeyStore.getInstance(provider)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        if (pubKey) {
            keyStore.getCertificate(keyName)?.let { return it.publicKey as PublicKey }
        } else {
            keyStore.getKey(keyName, null)?.let { return it as PrivateKey }
        }
        // if you reach here, then a new PrivateKey must be generated for that keyName
        val paramsBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramsBuilder.apply {
            setBlockModes(ASYMMETRIC_ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(ASYMMETRIC_ENCRYPTION_PADDING)
            setKeySize(RSA_KEY_SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setUserAuthenticationRequired(true)
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                setInvalidatedByBiometricEnrollment(true)
            }
            setUserAuthenticationValidityDurationSeconds(-1)
        }

        val keyGenParams = paramsBuilder.build()
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            provider
        )
        keyPairGenerator.initialize(keyGenParams)
        val keyPair = keyPairGenerator.genKeyPair()
        return if (pubKey) {
            keyPair.public
        } else {
            keyPair.private
        }
    }

    override fun persistEncryptedPasscodeToSharedPrefs(
        passcodeCiphertextWrapper: PasscodeCiphertextWrapper,
        filename: String,
        mode: Int,
        prefKey: String
    ) {
        val jsonAdapter = moshi.adapter(PasscodeCiphertextWrapper::class.java)
        val json = jsonAdapter.toJson(passcodeCiphertextWrapper)
        context.getSharedPreferences(filename, mode).edit().putString(prefKey, json).apply()
    }

    override fun retrieveEncryptedPasscodeFromSharedPrefs(
        filename: String,
        mode: Int,
        prefKey: String
    ): PasscodeCiphertextWrapper? {
        val jsonAdapter = moshi.adapter<PasscodeCiphertextWrapper>(PasscodeCiphertextWrapper::class.java)
        val json = context.getSharedPreferences(filename, mode).getString(prefKey, null)
        return json?.let { jsonAdapter.fromJson(it) }
    }
}

interface BiometricPasscodeManager {

    fun getInitializedRSACipherForEncryption(keyName: String): Cipher

    fun getInitializedRSACipherForDecryption(keyName: String): Cipher

    fun deleteKey(keyName: String)

    fun encryptData(plaintext: String, cipher: Cipher): PasscodeCiphertextWrapper

    fun decryptData(ciphertext: ByteArray, cipher: Cipher): String

    fun persistEncryptedPasscodeToSharedPrefs(
        passcodeCiphertextWrapper: PasscodeCiphertextWrapper,
        filename: String,
        mode: Int,
        prefKey: String
    )

    fun retrieveEncryptedPasscodeFromSharedPrefs(
        filename: String,
        mode: Int,
        prefKey: String
    ): PasscodeCiphertextWrapper?

    companion object {
        const val KEY_NAME = "prefs.string.passcode.biometrics.key_name"
        const val FILE_NAME = "prefs.string.passcode.biometrics.file_name"
    }
}

@JsonClass(generateAdapter = true)
data class PasscodeCiphertextWrapper(
    @Json(name = "ciphertext") val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }
}
