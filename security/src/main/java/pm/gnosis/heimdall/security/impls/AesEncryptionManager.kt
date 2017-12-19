package pm.gnosis.heimdall.security.impls

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.modes.CBCBlockCipher
import org.spongycastle.crypto.paddings.PKCS7Padding
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.base.TrackingActivityLifecycleCallbacks
import pm.gnosis.heimdall.common.utils.edit
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.EncryptionManager.CryptoData
import pm.gnosis.heimdall.security.exceptions.DeviceIsLockedException
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.toHexString
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesEncryptionManager @Inject constructor(
        application: Application,
        private val preferencesManager: PreferencesManager
) : EncryptionManager {

    private val secureRandom = SecureRandom()
    private val keyLock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var key: SecretKey? = null
    private var lockRunnable: Runnable? = null

    init {
        application.registerActivityLifecycleCallbacks(object : TrackingActivityLifecycleCallbacks() {

            override fun active() {
                lockRunnable?.let {
                    handler.removeCallbacks(lockRunnable)
                }
                lockRunnable = null
            }

            override fun inactive() {
                val runnable = Runnable { lock() }
                handler.postDelayed(runnable, LOCK_DELAY_MS)
                lockRunnable = runnable
            }

        })
    }

    private fun randomIv(): ByteArray {
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)
        return randomBytes
    }

    override fun initialized(): Single<Boolean> {
        return Single.fromCallable {
            preferencesManager.prefs.getString(PREF_KEY_ENCRYPTION_KEY, null) != null
        }
    }

    override fun setupPassword(newKeyBytes: ByteArray, oldKeyBytes: ByteArray?): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keyLock) {
                val checksum = preferencesManager.prefs.getString(PREF_KEY_PASSWORD_CHECKSUM, null)
                var previousKey: SecretKey? = null
                if (checksum != null) {
                    previousKey = oldKeyBytes?.let { buildSecretKeyIfValid(it, checksum) } ?: return@fromCallable false
                }
                val passwordKey = buildSecretKey(newKeyBytes)
                val encryptionKeyBytes = previousKey?.let {
                    decryptKey(it)
                } ?: generateKey()
                key = buildSecretKey(encryptionKeyBytes)
                key?.let {
                    preferencesManager.prefs.edit { putString(PREF_KEY_ENCRYPTION_KEY, encrypt(passwordKey, encryptionKeyBytes).toString()) }
                    preferencesManager.prefs.edit { putString(PREF_KEY_PASSWORD_CHECKSUM, generateCryptedChecksum(passwordKey, newKeyBytes)) }
                }

                key != null
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun generateKey(): ByteArray {
        val generatedPassword = ByteArray(32)
        secureRandom.nextBytes(generatedPassword)
        return generatedPassword
    }

    override fun unlocked(): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keyLock) {
                key != null
            }
        }
    }

    override fun unlockWithPassword(keyBytes: ByteArray): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keyLock) {
                // If we have no password set (no checksum stored, we cannot unlockWithPassword
                val checksum = preferencesManager.prefs.getString(PREF_KEY_PASSWORD_CHECKSUM, null) ?: return@fromCallable false
                val passwordKey = buildSecretKeyIfValid(keyBytes, checksum) ?: return@fromCallable false
                key = buildSecretKey(decryptKey(passwordKey) ?: return@fromCallable false)
                key != null
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun decryptKey(key: SecretKey): ByteArray? {
        val encryptedKey = preferencesManager.prefs.getString(PREF_KEY_ENCRYPTION_KEY, null) ?: return null
        return decrypt(key, CryptoData.fromString(encryptedKey))
    }

    override fun lock() {
        synchronized(keyLock) {
            key = null
        }
    }

    override fun decrypt(data: CryptoData): ByteArray {
        val key = synchronized(keyLock) {
            this.key ?: throw DeviceIsLockedException()
        }
        return decrypt(key, data)
    }

    override fun encrypt(data: ByteArray): CryptoData {
        val key = synchronized(keyLock) {
            this.key ?: throw DeviceIsLockedException()
        }
        return encrypt(key, data)
    }

    private fun keyChecksum(keyBytes: ByteArray) =
            Sha3Utils.sha3String(keyBytes).substring(0, 6).toByteArray()

    private fun buildSecretKeyIfValid(keyBytes: ByteArray?, checksum: String): SecretKey? {
        keyBytes ?: return null
        val secretKey = buildSecretKey(keyBytes)
        val decryptedChecksum = nullOnThrow { decrypt(secretKey, CryptoData.fromString(checksum)).toHexString() }
        if (keyChecksum(keyBytes).toHexString() == decryptedChecksum) {
            return secretKey
        }
        return null
    }

    private fun generateCryptedChecksum(key: SecretKey, keyBytes: ByteArray): String {
        return encrypt(key, keyChecksum(keyBytes)).toString()
    }

    private fun encrypt(key: SecretKey, data: ByteArray): CryptoData {
        return useCipher(true, key, CryptoData(data, randomIv()))
    }

    private fun decrypt(key: SecretKey, data: CryptoData): ByteArray {
        return useCipher(false, key, data).data
    }

    private fun useCipher(encrypt: Boolean, key: SecretKey, wrapper: CryptoData): CryptoData {
        val padding = PKCS7Padding()
        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()), padding)
        cipher.reset()

        val keyParam = KeyParameter(key.encoded)
        val params = ParametersWithIV(keyParam, wrapper.iv)
        cipher.init(encrypt, params)

        // create a temporary buffer to decode into (it'll include padding)
        val buf = ByteArray(cipher.getOutputSize(wrapper.data.size))
        var len = cipher.processBytes(wrapper.data, 0, wrapper.data.size, buf, 0)
        len += cipher.doFinal(buf, len)

        // remove padding
        val out = ByteArray(len)
        System.arraycopy(buf, 0, out, 0, len)

        return CryptoData(out, wrapper.iv)
    }

    companion object {
        private const val LOCK_DELAY_MS = 5 * 60 * 1000L
        private const val KEY_SPEC_ALGORITHM = "AES"
        private const val PREF_KEY_ENCRYPTION_KEY = "encryption_manager.string.encryption_key"
        private const val PREF_KEY_PASSWORD_CHECKSUM = "encryption_manager.string.password_checksum"

        private fun buildSecretKey(keyBytes: ByteArray): SecretKey {
            return SecretKeySpec(Sha3Utils.sha3(keyBytes), KEY_SPEC_ALGORITHM)
        }
    }
}