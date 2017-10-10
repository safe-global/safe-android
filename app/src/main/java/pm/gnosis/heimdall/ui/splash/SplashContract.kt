package pm.gnosis.heimdall.ui.splash

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.models.Account


abstract class SplashContract: ViewModel() {
    abstract fun initialSetup(): Completable
    abstract fun loadActiveAccount(): Single<Account>
}