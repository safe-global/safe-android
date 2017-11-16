package pm.gnosis.heimdall.ui.settings

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Single


abstract class SettingsContract: ViewModel() {
    abstract fun loadIpfsUrl(): Single<String>
    abstract fun updateIpfsUrl(url: String): Completable
    
    abstract fun loadRpcUrl(): Single<String>
    abstract fun updateRpcUrl(url: String): Completable
}