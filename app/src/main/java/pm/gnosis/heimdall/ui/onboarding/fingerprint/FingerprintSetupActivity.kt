package pm.gnosis.heimdall.ui.onboarding.fingerprint

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_fingerprint_setup.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.setColorFilterCompat
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.svalinn.security.AuthenticationError
import timber.log.Timber
import javax.inject.Inject

class FingerprintSetupActivity : ViewModelActivity<FingerprintSetupContract>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.FINGERPRINT

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

        disposables += toolbarHelper.setupShadow(layout_fingerprint_setup_toolbar_shadow, layout_fingerprint_setup_content_scroll)
    }

    private fun onFingerprintResult(isSuccessful: Boolean) {
        if (isSuccessful) {
            layout_fingerprint_setup_image.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fingerprint_confirmed))
            layout_fingerprint_setup_info_title.text = getString(R.string.fingerprint_confirmed)
            layout_fingerprint_setup_continue_label.text = getString(R.string.finish)
            layout_fingerprint_setup_description.visible(false)
            layout_fingerprint_setup_bottom_bar.setBackgroundColor(getColorCompat(R.color.azure))
            layout_fingerprint_setup_continue_label.setTextColor(getColorCompat(R.color.white))
            layout_fingerprint_setup_next_arrow.setColorFilterCompat(R.color.white)
        } else {
            snackbar(layout_fingerprint_setup_coordinator, R.string.fingerprint_not_recognized)
            vibrate(200)
        }
    }

    private fun onFingerprintUnrecoverableError(throwable: Throwable) {
        Timber.e(throwable)
        (throwable as? AuthenticationError)?.errString ?: getString(R.string.unknown_error).let { toast(it) }
        startActivity(SafeMainActivity.createIntent(this), true)
    }

    override fun layout() = R.layout.layout_fingerprint_setup

    override fun inject(component: ViewComponent) = viewComponent().inject(this)

    companion object {
        fun createIntent(context: Context) = Intent(context, FingerprintSetupActivity::class.java)
    }
}
