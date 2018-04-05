package pm.gnosis.heimdall.ui.onboarding.account

import android.arch.lifecycle.ViewModel
import com.google.android.gms.auth.api.credentials.Credential
import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class AccountSetupContract : ViewModel() {
    abstract fun continueWithGoogle(): Single<Result<Unit>>
    abstract fun setAccountFromCredential(credential: Credential): Completable
    abstract fun continueStoreFlow(): Completable
}
