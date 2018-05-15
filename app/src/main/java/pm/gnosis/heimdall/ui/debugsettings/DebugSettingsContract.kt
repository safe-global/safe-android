package pm.gnosis.heimdall.ui.debugsettings

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable

abstract class DebugSettingsContract : ViewModel() {
    abstract fun forceSyncAuthentication()
    abstract fun pair(payload: String): Completable
    abstract fun sendTestSafeCreationPush(): Completable
}
