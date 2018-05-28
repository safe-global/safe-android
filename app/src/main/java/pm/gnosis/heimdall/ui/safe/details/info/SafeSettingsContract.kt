package pm.gnosis.heimdall.ui.safe.details.info

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.GnosisSafeModulesRepository.Module
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result

abstract class SafeSettingsContract : ViewModel() {
    abstract fun setup(address: Solidity.Address)
    abstract fun getSafeAddress(): Solidity.Address
    abstract fun loadSafeInfo(ignoreCache: Boolean): Observable<Result<SafeInfo>>
    abstract fun loadSafeName(): Single<String>
    abstract fun updateSafeName(name: String): Single<Result<String>>
    abstract fun deleteSafe(): Single<Result<Unit>>
    abstract fun loadModulesInfo(extensions: List<Solidity.Address>): Single<Pair<Boolean, List<Pair<Module, Solidity.Address>>>>
}
