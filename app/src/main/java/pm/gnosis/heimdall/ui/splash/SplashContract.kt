package pm.gnosis.heimdall.ui.splash

import android.arch.lifecycle.ViewModel
import io.reactivex.Single

abstract class SplashContract : ViewModel() {
    abstract fun initialSetup(): Single<ViewAction>
}

sealed class ViewAction
class StartMain : ViewAction()
class StartAccountSetup : ViewAction()
class StartPasswordSetup : ViewAction()
