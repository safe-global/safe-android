package pm.gnosis.heimdall.ui.security.unlock

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.FingerprintUnlockResult
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import javax.inject.Inject

class UnlockViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val encryptionManager: EncryptionManager
) : UnlockContract() {

    override fun watchFingerprint(): Observable<Result<FingerprintUnlockResult>> =
            encryptionManager.watchFingerprintForUnlock()
                    .mapToResult()

    override fun checkState() =
            encryptionManager.unlocked()
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
}
