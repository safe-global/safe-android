package io.gnosis.safe.ui.settings.app.passcode

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.nio.charset.Charset
import java.security.*
import javax.crypto.Cipher

interface CryptographyManager {

    fun getInitializedCipherForEncryption(keyName: String): Cipher

    fun getInitializedCipherForDecryption(keyName: String): Cipher

    fun deleteKey(keyName: String)

    fun encryptData(plaintext: String, cipher: Cipher): CiphertextWrapper

    fun decryptData(ciphertext: ByteArray, cipher: Cipher): String

    fun persistCiphertextWrapperToSharedPrefs(
        ciphertextWrapper: CiphertextWrapper,
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    )

    fun getCiphertextWrapperFromSharedPrefs(
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    ): CiphertextWrapper?

    companion object {
        internal const val KEY_NAME = "prefs.string.passcode.biometrics.key_name611"
        internal const val FILE_NAME = "prefs.string.passcode.biometrics.file_name711"
    }
}

fun CryptographyManager(): CryptographyManager = CryptographyManagerImpl()

private class CryptographyManagerImpl : CryptographyManager {

    private val KEY_SIZE = 1024

    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA
    private val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_ECB
    private val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
    private val moshi = Moshi.Builder().build()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun getInitializedCipherForEncryption(keyName: String): Cipher {
        val cipher = getCipher()
        val publicKey = getOrCreateKey(keyName, true) as PublicKey
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun getInitializedCipherForDecryption(
        keyName: String
    ): Cipher {
        val cipher = getCipher()
        val privateKey = getOrCreateKey(keyName) as PrivateKey
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher
    }

    override fun deleteKey(keyName: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        keyStore.deleteEntry(keyName)
    }

    override fun encryptData(plaintext: String, cipher: Cipher): CiphertextWrapper {
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charset.forName("UTF-8")))
        return CiphertextWrapper(ciphertext)
    }

    override fun decryptData(ciphertext: ByteArray, cipher: Cipher): String {
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charset.forName("UTF-8"))
    }

    private fun getCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getOrCreateKey(keyName: String, pubKey: Boolean = false): Key {
        // If Secretkey was previously created for that keyName, then grab and return it.
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        if (pubKey) {
            keyStore.getCertificate(keyName)?.let { return it.publicKey as PublicKey }
        }
        keyStore.getKey(keyName, null)?.let { return it as PrivateKey }

        // if you reach here, then a new PrivateKey must be generated for that keyName
        val paramsBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramsBuilder.apply {
            setBlockModes(ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(ENCRYPTION_PADDING)
            setKeySize(KEY_SIZE)
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
            ANDROID_KEYSTORE
        )
        keyPairGenerator.initialize(keyGenParams)
        val keyPair = keyPairGenerator.genKeyPair()
        return if (pubKey) {
            keyPair.public
        } else {
            keyPair.private
        }
    }

    override fun persistCiphertextWrapperToSharedPrefs(
        ciphertextWrapper: CiphertextWrapper,
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    ) {
        val jsonAdapter = moshi.adapter(CiphertextWrapper::class.java)
        val json = jsonAdapter.toJson(ciphertextWrapper)
        context.getSharedPreferences(filename, mode).edit().putString(prefKey, json).apply()
    }

    override fun getCiphertextWrapperFromSharedPrefs(
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    ): CiphertextWrapper? {
        val jsonAdapter = moshi.adapter<CiphertextWrapper>(CiphertextWrapper::class.java)
        val json = context.getSharedPreferences(filename, mode).getString(prefKey, null)
        return jsonAdapter.fromJson(json)
    }
}

@JsonClass(generateAdapter = true)
data class CiphertextWrapper(
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
