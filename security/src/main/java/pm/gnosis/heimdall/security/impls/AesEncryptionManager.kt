package pm.gnosis.heimdall.security.impls

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.base.TrackingActivityLifecycleCallbacks
import pm.gnosis.heimdall.common.utils.edit
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.exceptions.DeviceIsLockedException
import pm.gnosis.utils.generateRandomString
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.toHexString
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesEncryptionManager @Inject constructor(
        application: Application,
        private val preferencesManager: PreferencesManager
) : EncryptionManager {

    private val ivSpec: IvParameterSpec
    private val keySpecLock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var keySpec: SecretKeySpec? = null
    private var lockRunnable: Runnable? = null

    init {
        val iv = preferencesManager.prefs.getString(PREF_KEY_INSTANCE_ID, null) ?: setupInstanceId()
        ivSpec = IvParameterSpec(iv.hexStringToByteArray())

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
        SecureRandom().nextBytes(randomBytes)
        val instanceId = randomBytes.toHexString()
        preferencesManager.prefs.edit {
            putString(PREF_KEY_INSTANCE_ID, instanceId)
        }
        return instanceId
    }

    override fun initialized(): Single<Boolean> {
        return Single.fromCallable {
            preferencesManager.prefs.getString(PREF_KEY_CHECKSUM, null) != null
        }
    }

    override fun setup(newKey: ByteArray, oldKey: ByteArray?): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keySpecLock) {
                val checksum = preferencesManager.prefs.getString(PREF_KEY_CHECKSUM, null)
                if (checksum != null) {
                    buildKeySpecChecksum(oldKey, checksum) ?: return@fromCallable false
                }
                keySpec = buildKeySpec(newKey)
                keySpec?.let {
                    preferencesManager.prefs.edit { putString(PREF_KEY_CHECKSUM, generateCryptedChecksum(it, newKey)) }
                }

                keySpec != null
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun unlocked(): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keySpecLock) {
                keySpec != null
            }
        }
    }

    override fun unlock(key: ByteArray): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keySpecLock) {
                // If we have no password set (no checksum stored, we cannot unlock
                val checksum = preferencesManager.prefs.getString(PREF_KEY_CHECKSUM, null) ?: return@fromCallable false
                keySpec = buildKeySpecChecksum(key, checksum)
                keySpec != null
            }
        }.subscribeOn(Schedulers.io())
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
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    private fun decrypt(keySpec: SecretKeySpec, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    private fun buildKeySpecChecksum(key: ByteArray?, checksum: String): SecretKeySpec? {
        key ?: return null
        val keySpec = buildKeySpec(key) ?: return null
        if (generateCryptedChecksum(keySpec, key) == checksum) {
            return keySpec
        }
        return null
    }

    companion object {
        private const val LOCK_DELAY_MS = 5 * 60 * 1000L
        private const val KEY_SPEC_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val PREF_KEY_INSTANCE_ID = "encryption_manager.string.instance_id"
        private const val PREF_KEY_CHECKSUM = "encryption_manager.string.checksum"

        private fun buildKeySpec(key: ByteArray?): SecretKeySpec? {
            key ?: return null
            return SecretKeySpec(Sha3Utils.sha3(key), KEY_SPEC_ALGORITHM)
        }
    }
}