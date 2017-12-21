package pm.gnosis.heimdall.security

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.crypto.Cipher

interface FingerprintHelper {
    fun removeKey(): Completable
    fun systemHasFingerprintsEnrolled(): Boolean
    fun isKeySet(): Single<Boolean>
    fun authenticate(iv: ByteArray? = null): Observable<AuthenticationResult>
}

sealed class AuthenticationResult
class AuthenticationFailed : AuthenticationResult()
data class AuthenticationError(val errMsgId: Int, val errString: CharSequence?) : IllegalArgumentException()
data class AuthenticationHelp(val helpMsgId: Int, val helpString: CharSequence?) : AuthenticationResult()
data class AuthenticationResultSuccess(val cipher: Cipher) : AuthenticationResult()
class FingerprintNotAvailable(message: String? = null) : Exception(message)
