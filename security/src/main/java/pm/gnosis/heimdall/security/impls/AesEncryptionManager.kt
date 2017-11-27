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
import pm.gnosis.heimdall.security.exceptions.DeviceIsLockedException
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.toHexString
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesEncryptionManager @Inject constructor(
        application: Application,
        private val preferencesManager: PreferencesManager
) : EncryptionManager {

    private val secureRandom = SecureRandom()
    private val ivData: ByteArray
    private val keySpecLock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var keySpec: SecretKeySpec? = null
    private var lockRunnable: Runnable? = null

    init {
        val iv = preferencesManager.prefs.getString(PREF_KEY_IV, null) ?: setupInstanceId()
        ivData = iv.hexStringToByteArray()

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

    private fun setupInstanceId(): String {
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)
        val instanceId = randomBytes.toHexString()
        preferencesManager.prefs.edit {
            putString(PREF_KEY_IV, instanceId)
        }
        return instanceId
    }

    override fun initialized(): Single<Boolean> {
        return Single.fromCallable {
            preferencesManager.prefs.getString(PREF_KEY_ENCRYPTION_KEY, null) != null
        }
    }

    override fun setupPassword(newKey: ByteArray, oldKey: ByteArray?): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keySpecLock) {
                val checksum = preferencesManager.prefs.getString(PREF_KEY_PASSWORD_CHECKSUM, null)
                var previousKeySpec: SecretKeySpec? = null
                if (checksum != null) {
                    previousKeySpec = oldKey?.let { buildKeySpecChecksum(it, checksum) } ?: return@fromCallable false
                }
                val passwordKeySpec = buildKeySpec(newKey)
                val generatedPassword = previousKeySpec?.let {
                    decryptKey(it)
                } ?: generatePassword()
                keySpec = buildKeySpec(generatedPassword)
                keySpec?.let {
                    preferencesManager.prefs.edit { putString(PREF_KEY_ENCRYPTION_KEY, encrypt(passwordKeySpec, generatedPassword).toHexString()) }
                    preferencesManager.prefs.edit { putString(PREF_KEY_PASSWORD_CHECKSUM, generateCryptedChecksum(passwordKeySpec, newKey)) }
                }

                keySpec != null
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun generatePassword(): ByteArray {
        val generatedPassword = ByteArray(32)
        secureRandom.nextBytes(generatedPassword)
        return generatedPassword
    }

    override fun unlocked(): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keySpecLock) {
                keySpec != null
            }
        }
    }

    override fun unlockWithPassword(key: ByteArray): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keySpecLock) {
                // If we have no password set (no checksum stored, we cannot unlockWithPassword
                val checksum = preferencesManager.prefs.getString(PREF_KEY_PASSWORD_CHECKSUM, null) ?: return@fromCallable false
                val passwordKeySpec = buildKeySpecChecksum(key, checksum) ?: return@fromCallable false
                keySpec = buildKeySpec(decryptKey(passwordKeySpec) ?: return@fromCallable false)
                keySpec != null
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun decryptKey(keySpec: SecretKeySpec): ByteArray? {
        val encryptedKey = preferencesManager.prefs.getString(PREF_KEY_ENCRYPTION_KEY, null) ?: return null
        return decrypt(keySpec, encryptedKey.hexStringToByteArray())
    }

    override fun lock() {
        synchronized(keySpecLock) {
            keySpec = null
        }
    }

    override fun decrypt(data: ByteArray): ByteArray {
        val keySpec = synchronized(keySpecLock) {
            this.keySpec ?: throw DeviceIsLockedException()
        }
        return decrypt(keySpec, data)
    }

    override fun encrypt(data: ByteArray): ByteArray {
        val keySpec = synchronized(keySpecLock) {
            this.keySpec ?: throw DeviceIsLockedException()
        }
        return encrypt(keySpec, data)
    }

    private fun generateCryptedChecksum(keySpec: SecretKeySpec, key: ByteArray): String {
        return encrypt(keySpec, Sha3Utils.sha3String(key).substring(0, 6).toByteArray()).toHexString()
    }

    private fun encrypt(keySpec: SecretKeySpec, data: ByteArray): ByteArray {
        return useCipher(true, keySpec, data)
    }

    private fun decrypt(keySpec: SecretKeySpec, data: ByteArray): ByteArray {
        return useCipher(false, keySpec, data)
    }

    private fun useCipher(encryt: Boolean, keySpec: SecretKeySpec, data: ByteArray): ByteArray {
        val padding = PKCS7Padding()
        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()), padding)
        cipher.reset()

        val keyParam = KeyParameter(keySpec.encoded)
        val params = ParametersWithIV(keyParam, ivData)
        cipher.init(encryt, params)

        // create a temporary buffer to decode into (it'll include padding)
        val buf = ByteArray(cipher.getOutputSize(data.size))
        var len = cipher.processBytes(data, 0, data.size, buf, 0)
        len += cipher.doFinal(buf, len)

        // remove padding
        val out = ByteArray(len)
        System.arraycopy(buf, 0, out, 0, len)

        return out
    }

    private fun buildKeySpecChecksum(key: ByteArray?, checksum: String): SecretKeySpec? {
        key ?: return null
        val keySpec = buildKeySpec(key)
        if (generateCryptedChecksum(keySpec, key) == checksum) {
            return keySpec
        }
        return null
    }

    companion object {
        private const val LOCK_DELAY_MS = 5 * 60 * 1000L
        private const val KEY_SPEC_ALGORITHM = "AES"
        private const val PREF_KEY_IV = "encryption_manager.string.iv"
        private const val PREF_KEY_ENCRYPTION_KEY = "encryption_manager.string.encryption_key"
        private const val PREF_KEY_PASSWORD_CHECKSUM = "encryption_manager.string.password_checksum"

        private fun buildKeySpec(key: ByteArray): SecretKeySpec {
            return SecretKeySpec(Sha3Utils.sha3(key), KEY_SPEC_ALGORITHM)
        }
    }
}