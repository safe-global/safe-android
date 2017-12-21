package pm.gnosis.heimdall.security.impls

import android.app.Application
import android.app.KeyguardManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.reactivex.Completable
import io.reactivex.Observable
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
import pm.gnosis.heimdall.security.*
import pm.gnosis.heimdall.security.EncryptionManager.CryptoData
import pm.gnosis.heimdall.security.exceptions.DeviceIsLockedException
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.toHexString
import java.security.SecureRandom
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesEncryptionManager @Inject constructor(
        private val application: Application,
        private val preferencesManager: PreferencesManager,
        private val fingerprintHelper: FingerprintHelper
) : EncryptionManager {

    private val secureRandom = SecureRandom()
    private val keyLock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var key: ByteArray? = null
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
            preferencesManager.prefs.getString(PREF_KEY_PASSWORD_ENCRYPTED_APP_KEY, null) != null
        }
    }

    override fun setupPassword(newPassword: ByteArray, oldPassword: ByteArray?): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keyLock) {
                val checksum = preferencesManager.prefs.getString(PREF_KEY_PASSWORD_CHECKSUM, null)
                var previousKey: ByteArray? = null
                if (checksum != null) {
                    previousKey = buildPasswordKeyIfValid(oldPassword, checksum) ?: return@fromCallable false
                }
                val passwordKey = Sha3Utils.sha3(newPassword)
                key = previousKey?.let {
                    decryptAppKey(it)
                } ?: generateKey()
                key?.let {
                    preferencesManager.prefs.edit { putString(PREF_KEY_PASSWORD_ENCRYPTED_APP_KEY, encrypt(passwordKey, it).toString()) }
                    preferencesManager.prefs.edit { putString(PREF_KEY_PASSWORD_CHECKSUM, generateCryptedChecksum(passwordKey)) }
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

    override fun unlockWithPassword(password: ByteArray): Single<Boolean> {
        return Single.fromCallable {
            synchronized(keyLock) {
                // If we have no password set (no checksum stored, we cannot unlockWithPassword
                val checksum = preferencesManager.prefs.getString(PREF_KEY_PASSWORD_CHECKSUM, null) ?: return@fromCallable false
                val passwordKey = buildPasswordKeyIfValid(password, checksum) ?: return@fromCallable false
                key = decryptAppKey(passwordKey) ?: return@fromCallable false
                key != null
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun decryptAppKey(key: ByteArray): ByteArray? {
        val encryptedKey = preferencesManager.prefs.getString(PREF_KEY_PASSWORD_ENCRYPTED_APP_KEY, null) ?: return null
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

    private fun keyChecksum(key: ByteArray) =
            Sha3Utils.sha3String(key).substring(0, 6).toByteArray()

    private fun buildPasswordKeyIfValid(key: ByteArray?, checksum: String): ByteArray? {
        key ?: return null
        val hashedKey = Sha3Utils.sha3(key)
        val decryptedChecksum = nullOnThrow { decrypt(hashedKey, CryptoData.fromString(checksum)).toHexString() }
        if (keyChecksum(hashedKey).toHexString() == decryptedChecksum) {
            return hashedKey
        }
        return null
    }

    private fun generateCryptedChecksum(key: ByteArray): String {
        return encrypt(key, keyChecksum(key)).toString()
    }

    private fun encrypt(key: ByteArray, data: ByteArray): CryptoData {
        return useCipher(true, key, CryptoData(data, randomIv()))
    }

    private fun decrypt(key: ByteArray, data: CryptoData): ByteArray {
        return useCipher(false, key, data).data
    }

    override fun canSetupFingerprint() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    nullOnThrow { application.getSystemService(KeyguardManager::class.java).isKeyguardSecure == true } ?: false &&
                    fingerprintHelper.systemHasFingerprintsEnrolled()

    override fun observeFingerprintForSetup(): Observable<Boolean> =
            fingerprintHelper.authenticate()
                    .subscribeOn(Schedulers.io())
                    .map { result ->
                        when (result) {
                            is AuthenticationResultSuccess -> {
                                key?.let { key ->
                                    preferencesManager.prefs.edit {
                                        val cryptoData = CryptoData(result.cipher.doFinal(key),
                                                result.cipher.parameters.getParameterSpec(IvParameterSpec::class.java).iv)
                                        putString(PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY, cryptoData.toString())
                                    }
                                    true
                                } ?: false
                            }
                            else -> false
                        }
                    }

    override fun observeFingerprintForUnlock(): Observable<FingerprintUnlockResult> =
            Observable
                    .fromCallable {
                        val cryptedData = preferencesManager.prefs.getString(PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY, null) ?: throw FingerprintUnlockError()
                        CryptoData.fromString(cryptedData)
                    }
                    .flatMap { cryptedData -> fingerprintHelper.authenticate(cryptedData.iv).map { cryptedData to it } }
                    .subscribeOn(Schedulers.io())
                    .map { (cryptedData, authResult) ->
                        when (authResult) {
                            is AuthenticationResultSuccess -> {
                                synchronized(keyLock) {
                                    key = authResult.cipher.doFinal(cryptedData.data)
                                }
                                if (key != null) FingerprintUnlockSuccessful() else throw FingerprintUnlockError()
                            }
                            is AuthenticationFailed -> FingerprintUnlockFailed()
                            is AuthenticationHelp -> FingerprintUnlockHelp(authResult.helpString)
                        }
                    }

    override fun isFingerPrintSet(): Single<Boolean> =
            fingerprintHelper.isKeySet()
                    .map { it && preferencesManager.prefs.getString(PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY, null) != null }

    override fun clearFingerprintData(): Completable =
            Completable.fromCallable {
                preferencesManager.prefs.edit {
                    remove(PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY)
                }
            }.andThen(fingerprintHelper.removeKey())

    private fun useCipher(encrypt: Boolean, key: ByteArray, wrapper: CryptoData): CryptoData {
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

        return CryptoData(out, wrapper.iv)
    }

    companion object {
        private const val LOCK_DELAY_MS = 5 * 60 * 1000L
        private const val PREF_KEY_PASSWORD_ENCRYPTED_APP_KEY = "encryption_manager.string.password_encrypted_app_key"
        private const val PREF_KEY_PASSWORD_CHECKSUM = "encryption_manager.string.password_checksum"
        private const val PREF_KEY_FINGERPRINT_ENCRYPTED_APP_KEY = "encryption_manager.string.fingerprint_encrypted_app_key"
    }
}
