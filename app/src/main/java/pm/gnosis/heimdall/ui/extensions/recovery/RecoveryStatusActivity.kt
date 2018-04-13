package pm.gnosis.heimdall.ui.extensions.recovery


import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.model.Solidity

abstract class RecoveryStatusContract : ViewModel() {
    abstract fun loadRecoveryStatus()
}

class RecoveryStatusActivity : ViewModelActivity<RecoveryStatusContract>() {

    override fun screenId() = ScreenId.RECOVERY_ACTIVITY

    override fun layout() = R.layout.layout_recovery_status

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        fun createIntent(context: Context, extension: Solidity.Address) = Intent(context, RecoveryStatusActivity::class.java)
    }
}
