package pm.gnosis.heimdall.ui.extensions.recovery


import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_recovery_status.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ViewComponent
import pm.gnosis.heimdall.data.repositories.GnosisSafeExtensionRepository
import pm.gnosis.heimdall.data.repositories.ReplaceSafeOwnerData
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.model.Solidity
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RecoveryStatusViewModel @Inject constructor(
    private val extensionRepository: GnosisSafeExtensionRepository
) : RecoveryStatusContract() {
    override fun loadRecoveryStatus(): Single<RecoveryStatus> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

abstract class RecoveryStatusContract : ViewModel() {
    abstract fun loadRecoveryStatus(): Single<RecoveryStatus>

    sealed class RecoveryStatus {
        class NotTriggered(val recoveryOwner: Solidity.Address) : RecoveryStatus()
        data class Triggered(val recoveryOwner: Solidity.Address, val triggerTime: Long, val replaceData: ReplaceSafeOwnerData) : RecoveryStatus()
        data class WaitingForCompletion(val recoveryOwner: Solidity.Address, val triggerTime: Long, val replaceData: ReplaceSafeOwnerData) :
            RecoveryStatus()
    }
}

class RecoveryStatusActivity : ViewModelActivity<RecoveryStatusContract>() {

    override fun screenId() = ScreenId.RECOVERY_ACTIVITY

    override fun layout() = R.layout.layout_recovery_status

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onStart() {
        super.onStart()
        disposables += Observable.intervalRange(1, 100, 1, 1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = {
                layout_recovery_status_delay_progress.max = 100
                layout_recovery_status_delay_progress.progress = it.toInt()
                layout_recovery_status_delay_status.text = it.toString()
            })
        layout_recovery_status_delay_progress.isIndeterminate = false
        layout_recovery_status_delay_progress.max = 100
        layout_recovery_status_delay_progress.progress = 0
    }

    companion object {
        fun createIntent(context: Context, extension: Solidity.Address) = Intent(context, RecoveryStatusActivity::class.java)
    }
}
