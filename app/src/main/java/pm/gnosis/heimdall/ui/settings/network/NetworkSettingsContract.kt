package pm.gnosis.heimdall.ui.settings.network

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class NetworkSettingsContract : ViewModel() {
    abstract fun loadIpfsUrl(): Single<String>
    abstract fun updateIpfsUrl(url: String): Single<Result<String>>

    abstract fun loadRpcUrl(): Single<String>
    abstract fun updateRpcUrl(url: String): Single<Result<String>>

    abstract fun loadProxyFactoryAddress(): Single<String>
    abstract fun updateProxyFactoryAddress(address: String): Single<Result<String>>
    abstract fun loadSafeMasterCopyAddress(): Single<String>
    abstract fun updateSafeMasterCopyAddress(address: String): Single<Result<String>>
}
