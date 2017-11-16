package pm.gnosis.heimdall.ui.settings

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result


abstract class SettingsContract: ViewModel() {
    abstract fun loadIpfsUrl(): Single<String>
    abstract fun updateIpfsUrl(url: String): Single<Result<String>>
    
    abstract fun loadRpcUrl(): Single<String>
    abstract fun updateRpcUrl(url: String): Single<Result<String>>
}