package pm.gnosis.heimdall.ui.settings.general

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_general_settings.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.settings.general.changepassword.PasswordChangeActivity
import pm.gnosis.heimdall.ui.settings.general.fingerprint.FingerprintDialog
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import timber.log.Timber
import javax.inject.Inject

class GeneralSettingsActivity : ViewModelActivity<GeneralSettingsContract>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    private val removeFingerprintClick = PublishSubject.create<Unit>()
    private val getFingerprintStateSubject = PublishSubject.create<Unit>()

    override fun screenId() = ScreenId.SETTINGS

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun layout() = R.layout.layout_general_settings

    override fun onStart() {
        super.onStart()
        if (viewModel.isFingerprintAvailable()) setupFingerprintAction()

        layout_general_settings_app_version.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        disposables += layout_general_settings_password_background.clicks()
            .subscribeBy(onNext = {
                startActivity(PasswordChangeActivity.createIntent(this))
            }, onError = Timber::e)

        disposables += layout_general_settings_tos_background.clicks()
            .subscribeBy(onNext = {
                openUrl(getString(R.string.tos_link))
            }, onError = Timber::e)

        disposables += layout_general_settings_privacy_background.clicks()
            .subscribeBy(onNext = {
                openUrl(getString(R.string.privacy_link))
            }, onError = Timber::e)

        disposables += layout_general_settings_rate_background.clicks()
            .subscribeBy(onNext = {
                openPlayStore()
            }, onError = Timber::e)

        disposables += layout_general_settings_feedback_background.clicks()
            .subscribeBy(onNext = {
                startActivity(GetInTouchActivity.newIntent(this))
            }, onError = Timber::e)

        disposables += layout_general_settings_licenses_background.clicks()
            .subscribeBy(onNext = {
                openUrl(getString(R.string.licenses_link))
            }, onError = Timber::e)

        disposables += layout_general_settings_back_button.clicks()
            .subscribeBy(onNext = {
                onBackPressed()
            }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_general_settings_toolbar_shadow, layout_general_settings_content_scroll)
    }

    private fun setupFingerprintAction() {
        layout_general_settings_fingerprint_group.visibility = View.VISIBLE
        disposables += layout_general_settings_fingerprint_background.clicks()
            .subscribeBy(onNext = {
                if (layout_general_settings_fingerprint_switch.isChecked) {
                    removeFingerprintClick.onNext(Unit)
                } else {
                    showFingerprintDialog()
                }
            }, onError = Timber::e)

        disposables += removeFingerprintClick
            .flatMapSingle { viewModel.clearFingerprintData() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                layout_general_settings_fingerprint_switch.isChecked = false
                layout_general_settings_fingerprint_info.text = getString(R.string.disabled)
                snackbar(layout_general_settings_fingerprint_switch, R.string.fingerprint_unlock_disabled)
            }, onError = Timber::e)

        disposables += getFingerprintStateSubject.flatMapSingle { encryptionManager.isFingerPrintSet() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = {
                layout_general_settings_fingerprint_switch.isChecked = it
                layout_general_settings_fingerprint_info.text = getString(if (it) R.string.enabled else R.string.disabled)
            }, onError = Timber::e)
        getFingerprintStateSubject.onNext(Unit)
    }

    private fun showFingerprintDialog() {
        val dialog = FingerprintDialog.create()
        dialog.show(supportFragmentManager, null)
        dialog.successListener = {
            getFingerprintStateSubject.onNext(Unit)
            if (it) {
                snackbar(layout_general_settings_fingerprint_switch, R.string.fingerprint_unlock_enabled)
            }
        }
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, GeneralSettingsActivity::class.java)
    }
}
