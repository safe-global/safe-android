package pm.gnosis.heimdall.ui.onboarding.fingerprint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_fingerprint_setup.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.colorStatusBar
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.startActivity
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.svalinn.common.utils.vibrate
import pm.gnosis.svalinn.security.AuthenticationError
import timber.log.Timber

class FingerprintSetupActivity : ViewModelActivity<FingerprintSetupContract>() {

    override fun screenId() = ScreenId.FINGERPRINT


    override fun onCreate(savedInstanceState: Bundle?) {
        window.colorStatusBar(R.color.safe_green)
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeFingerprintForSetup()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onFingerprintResult, onError = ::onFingerprintUnrecoverableError)

        disposables += layout_fingerprint_setup_continue.clicks()
            .subscribeBy(
                onNext = { startActivity(SafeMainActivity.createIntent(this), true) },
                onError = Timber::e
            )
    }

    private fun onFingerprintResult(isSuccessful: Boolean) {
        if (isSuccessful) {
            layout_fingerprint_setup_continue_label.text = getString(R.string.finish)
            snackbar(layout_fingerprint_setup_coordinator, R.string.fingerprint_confirmed)
            vibrate(200)
        } else {
            snackbar(layout_fingerprint_setup_coordinator, R.string.fingerprint_not_recognized)
            vibrate(200)
        }
    }

    private fun onFingerprintUnrecoverableError(throwable: Throwable) {
        Timber.e(throwable)
        toast(((throwable as? AuthenticationError)?.errString ?: getString(R.string.unknown_error)))
        startActivity(SafeMainActivity.createIntent(this), true)
    }

    override fun layout() = R.layout.layout_fingerprint_setup

    override fun inject(component: ViewComponent) = viewComponent().inject(this)

    companion object {
        fun createIntent(context: Context) = Intent(context, FingerprintSetupActivity::class.java)
    }
}
