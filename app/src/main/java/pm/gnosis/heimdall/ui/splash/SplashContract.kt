package pm.gnosis.heimdall.ui.splash

import androidx.lifecycle.ViewModel
import io.reactivex.Single

abstract class SplashContract : ViewModel() {
    abstract fun initialSetup(): Single<ViewAction>
}

sealed class ViewAction
object StartMain : ViewAction()
object StartPasswordSetup : ViewAction()
object StartUnlock : ViewAction()
