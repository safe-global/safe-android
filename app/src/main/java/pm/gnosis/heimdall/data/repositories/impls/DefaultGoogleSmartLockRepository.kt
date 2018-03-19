package pm.gnosis.heimdall.data.repositories.impls

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.CredentialsClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.repositories.GoogleSmartLockRepository
import pm.gnosis.heimdall.data.repositories.GoogleSmartLockRepository.Companion.GNOSIS_SAFE_CREDENTIAL_ID
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGoogleSmartLockRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialsClient: CredentialsClient
) : GoogleSmartLockRepository {
    override fun storeCredentials(mnemonicSeed: ByteArray): Completable =
        StoreCredentialsCompletable(mnemonicSeed.toHexString()).create().subscribeOn(Schedulers.io())

    override fun retrieveCredentials(): Single<Credential> = ReadCredentialsSingle().create().subscribeOn(Schedulers.io())

    private inner class StoreCredentialsCompletable(private val mnemonic: String) : CompletableOnSubscribe {
        override fun subscribe(e: CompletableEmitter) {
            val credential = Credential.Builder(GNOSIS_SAFE_CREDENTIAL_ID).setPassword(mnemonic).build()
            credentialsClient.save(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) e.onComplete()
                else {
                    task.exception?.let { exception ->
                        when {
                            exception is ResolvableApiException -> e.onError(
                                ActivityShouldRequestCredentialDialogException(exception, CredentialDialogAction.STORE)
                            )
                            exception is ApiException && exception.statusCode == 16 -> e.onError(NoAccountsAvailableWithSmartLockException())
                            else -> e.onError(exception)
                        }
                    }
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
                            exception is ResolvableApiException && exception.statusCode == 4 ->
                                // Could not find accounts with ethereum credentials
                                e.onError(NoCredentialsStoredException())
                            exception is ResolvableApiException ->
                                // This is most likely the case where the user has multiple saved
                                // credentials and needs to pick one. This requires showing UI to
                                // resolve the read request.
                                e.onError(ActivityShouldRequestCredentialDialogException(exception, CredentialDialogAction.READ))
                            exception is ApiException && exception.statusCode == 16 -> e.onError(NoAccountsAvailableWithSmartLockException())
                            else -> e.onError(exception)
                        }
                    }
                }
            }
        }

        fun create(): Single<Credential> = Single.create(this)
    }
}

class NoAccountsAvailableWithSmartLockException : Exception()
class NoCredentialsStoredException : Exception()

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
