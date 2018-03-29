package pm.gnosis.heimdall.ui.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.components.ViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.svalinn.security.EncryptionManager
import timber.log.Timber
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var encryptionManager: EncryptionManager

    @Inject
    lateinit var eventTracker: EventTracker

    protected val disposables = CompositeDisposable()

    private var performSecurityCheck = true

    abstract fun screenId(): ScreenId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HeimdallApplication[this].component.inject(this)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onStart() {
        super.onStart()
        if (performSecurityCheck) {
            disposables += encryptionManager.unlocked()
                // We block the ui thread here to avoid exposing the ui before the app is unlocked
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::checkSecurity, ::handleCheckError)
        }
        eventTracker.submit(Event.ScreenView(screenId()))
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    protected fun skipSecurityCheck() {
        performSecurityCheck = false
    }

    private fun checkSecurity(unlocked: Boolean) {
        if (!unlocked) {
            startActivity(UnlockActivity.createIntent(this))
        }
    }

    private fun handleCheckError(throwable: Throwable) {
        Timber.d(throwable)
        // Show blocker screen. No auth -> no app usage
    }

    protected fun viewComponent(): ViewComponent =
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build()
}
