package pm.gnosis.heimdall.helpers

import android.app.Activity
import android.content.IntentSender
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.CredentialsClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSmartLockHelper @Inject constructor(
    private val credentialsClient: CredentialsClient
) {
    fun storeCredentials(mnemonic: String): Completable =
        StoreCredentialsCompletable(mnemonic).create().subscribeOn(Schedulers.io())

    fun retrieveCredentials(): Single<Credential> = ReadCredentialsSingle().create().subscribeOn(Schedulers.io())

    private inner class StoreCredentialsCompletable(private val mnemonic: String) : CompletableOnSubscribe {
        override fun subscribe(e: CompletableEmitter) {
            val credential = Credential.Builder(GNOSIS_SAFE_CREDENTIAL_ID).setPassword(mnemonic).build()
            credentialsClient.save(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) e.onComplete()
                else {
                    task.exception?.let { exception ->
                        when {
                            exception is ResolvableApiException -> e.onError(
                                ActivityShouldRequestCredentialDialogException(
                                    exception,
                                    CredentialDialogAction.STORE
                                )
                            )
                            exception is ApiException && exception.statusCode == CommonStatusCodes.CANCELED -> e.onError(
                                NoAccountsAvailableWithSmartLockException()
                            )
                            else -> e.onError(exception)
                        }
                    } ?: e.onError(UnkownGoogleSmartLockException())
                }
            }
        }

        fun create(): Completable = Completable.create(this)
    }

    private inner class ReadCredentialsSingle : SingleOnSubscribe<Credential> {
        override fun subscribe(e: SingleEmitter<Credential>) {
            val credentialRequest = CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .build()

            credentialsClient.request(credentialRequest).addOnCompleteListener { task ->
                if (task.isSuccessful) e.onSuccess(task.result.credential)
                else {
                    task.exception?.let { exception ->
                        when {
                            exception is ResolvableApiException && exception.statusCode == CommonStatusCodes.SIGN_IN_REQUIRED ->
                                // Could not find accounts with ethereum credentials
                                e.onError(NoCredentialsStoredException())
                            exception is ResolvableApiException ->
                                // This is most likely the case where the user has multiple saved
                                // credentials and needs to pick one. This requires showing UI to
                                // resolve the read request.
                                e.onError(
                                    ActivityShouldRequestCredentialDialogException(
                                        exception,
                                        CredentialDialogAction.READ
                                    )
                                )
                            exception is ApiException && exception.statusCode == CommonStatusCodes.CANCELED -> e.onError(
                                NoAccountsAvailableWithSmartLockException()
                            )
                            else -> e.onError(exception)
                        }
                    } ?: e.onError(UnkownGoogleSmartLockException())
                }
            }
        }

        fun create(): Single<Credential> = Single.create(this)
    }

    companion object {
        const val GNOSIS_SAFE_CREDENTIAL_ID = "Gnosis Safe Account"
    }
}

class NoAccountsAvailableWithSmartLockException : Exception()
class NoCredentialsStoredException : Exception()
class UnkownGoogleSmartLockException : Exception("Task is not successful and error is null")

enum class CredentialDialogAction(val requestCode: Int) {
    STORE(10),
    READ(11)
}

class ActivityShouldRequestCredentialDialogException(private val rae: ResolvableApiException, private val action: CredentialDialogAction) :
    Exception() {
    fun request(activity: Activity) {
        try {
            rae.startResolutionForResult(activity, action.requestCode)
        } catch (e: IntentSender.SendIntentException) {
            Timber.e("Failed to send resolution.")
        }
    }
}
