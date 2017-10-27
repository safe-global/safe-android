package pm.gnosis.heimdall.ui.security

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import javax.inject.Inject


class SecurityViewModel @Inject constructor(
        private @ApplicationContext val context: Context,
        private val encryptionManager: EncryptionManager
) : SecurityContract() {

    override fun checkState(): Observable<Result<SecurityContract.State>> =
            encryptionManager.unlocked()
                    .flatMap({
                        if (it)
                            Single.just(State.UNLOCKED)
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

    override fun setupPin(pin: String, repeat: String): Observable<Result<State>> =
            checkPins(pin, repeat)
                    .flatMap {
                        encryptionManager.setup(pin.toByteArray()).toObservable()
                    }
                    .map {
                        LocalizedException.assert(it, context, R.string.pin_setup_failed)
                        State.UNLOCKED
                    }
                    .mapToResult()

    private fun checkPins(pin: String, repeat: String) =
            Observable.fromCallable {
                LocalizedException.assert(pin.length >= 5, context, R.string.pin_too_short)
                LocalizedException.assert(pin == repeat, context, R.string.pin_repeat_wrong)
                pin
            }

    override fun unlockPin(pin: String): Observable<Result<State>> =
            encryptionManager.unlock(pin.toByteArray())
                    .map {
                        LocalizedException.assert(it, context, R.string.error_wrong_credentials)
                        State.UNLOCKED
                    }.toObservable().mapToResult()
}