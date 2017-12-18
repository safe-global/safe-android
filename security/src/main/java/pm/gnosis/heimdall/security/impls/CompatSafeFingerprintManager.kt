package pm.gnosis.heimdall.security.impls

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v4.os.CancellationSignal
import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.utils.nullOnThrow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatSafeFingerprintManager @Inject constructor(
        @ApplicationContext private val context: Context
) {
    private val keyStore by lazy { KeyStore.getInstance(ANDROID_KEY_STORE) }
    private val keyGenerator by lazy { KeyGenerator.getInstance(AES, ANDROID_KEY_STORE) }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createKey(): SecretKey {
        val builder = KeyGenParameterSpec.Builder(FINGERPRINT_KEY,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        keyGenerator.init(builder.setKeySize(256).build())
        return keyGenerator.generateKey()
    }

    fun removeKey(): Completable = Completable.fromCallable {
        keyStore.load(null)
        keyStore.deleteEntry(FINGERPRINT_KEY)
    }.subscribeOn(Schedulers.io())

    fun systemHasFingerprintsEnrolled() =
            nullOnThrow { FingerprintManagerCompat.from(context).hasEnrolledFingerprints() } ?: false

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getOrCreateKey(): SecretKey {
        keyStore.load(null)
        return keyStore.getKey(FINGERPRINT_KEY, null) as? SecretKey ?: createKey()
    }

    fun isKeySet(): Single<Boolean> = Single.fromCallable {
        keyStore.load(null)
        keyStore.getKey(FINGERPRINT_KEY, null) != null
    }.subscribeOn(Schedulers.io())

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createCipher(iv: ByteArray?) =
            Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .apply {
                        if (iv == null) init(Cipher.ENCRYPT_MODE, getOrCreateKey())
                        else init(Cipher.DECRYPT_MODE, getOrCreateKey(), IvParameterSpec(iv))
                    }

    fun authenticate(iv: ByteArray? = null): Observable<AuthenticationResult> =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) Observable.empty()
            else Observable.create(AuthenticateObservable(context, iv, ::createCipher))

    private class AuthenticateObservable(context: Context, private val iv: ByteArray? = null,
                                         private val cipherProvider: (ByteArray?) -> Cipher)
        : FingerprintManagerCompat.AuthenticationCallback(), ObservableOnSubscribe<AuthenticationResult> {

        private val fingerprintManager = FingerprintManagerCompat.from(context)
        private var emitter: ObservableEmitter<AuthenticationResult>? = null

        override fun subscribe(emitter: ObservableEmitter<AuthenticationResult>) {
            try {
                val cryptoObject = FingerprintManagerCompat.CryptoObject(cipherProvider(iv))
                this.emitter = emitter
                val signal = CancellationSignal()
                fingerprintManager.authenticate(cryptoObject, 0, signal, this, null)
                emitter.setCancellable {
                    if (!signal.isCanceled) {
                        signal.cancel()
                        this.emitter = null
                    }
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            result?.let {
                emitter?.onNext(AuthenticationResultSuccess(it.cryptoObject.cipher))
                emitter?.onComplete()
            }
        }

        override fun onAuthenticationFailed() {
            emitter?.onNext(AuthenticationFailed())
        }

        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            emitter?.onError(AuthenticationError(errMsgId, errString))
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            emitter?.onNext(AuthenticationHelp(helpMsgId, helpString))
            emitter?.onComplete()
        }
    }

    companion object {
        private const val AES = "AES"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val FINGERPRINT_KEY = "GnosisFingerprintKey"
    }
}

data class AuthenticationError(val errMsgId: Int, val errString: CharSequence?) : IllegalArgumentException()

sealed class AuthenticationResult
class AuthenticationFailed : AuthenticationResult()
data class AuthenticationHelp(val helpMsgId: Int, val helpString: CharSequence?) : AuthenticationResult()
data class AuthenticationResultSuccess(val cipher: Cipher) : AuthenticationResult()
