package pm.gnosis.heimdall.ui.security.unlock

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.remote.PushServiceRepository
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.FingerprintUnlockResult
import javax.inject.Inject

class UnlockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager,
    private val pushServiceRepository: PushServiceRepository
) : UnlockContract() {

    override fun observeFingerprint(): Observable<Result<FingerprintUnlockResult>> =
        encryptionManager.observeFingerprintForUnlock()
            .mapToResult()

    override fun checkState(forceConfirmCredentials: Boolean) =
        (if (forceConfirmCredentials) Single.just(false) else encryptionManager.unlocked())
            .flatMap({
                if (it) Single.just(State.UNLOCKED)
                else
                    encryptionManager.initialized().map {
                        if (it)
                            State.LOCKED
                        else
                            State.UNINITIALIZED
                    }
            })
            .subscribeOn(Schedulers.io())
            .toObservable().mapToResult()

    override fun unlock(password: String) =
        encryptionManager.unlockWithPassword(password.toByteArray())
            .map {
                SimpleLocalizedException.assert(it, context, R.string.error_wrong_credentials)
                State.UNLOCKED
            }.toObservable().mapToResult()

    override fun syncPushAuthentication() = pushServiceRepository.syncAuthentication()
}
