package pm.gnosis.heimdall.ui.safe.details.info

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import java.math.BigInteger


abstract class SafeSettingsContract : ViewModel() {
    abstract fun setup(address: BigInteger)
    abstract fun getSafeAddress(): BigInteger
    abstract fun loadSafeInfo(ignoreCache: Boolean): Observable<Result<SafeInfo>>
    abstract fun loadSafeName(): Single<String>
    abstract fun updateSafeName(name: String): Single<Result<String>>
    abstract fun deleteSafe(): Single<Result<Unit>>
}