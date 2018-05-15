package pm.gnosis.heimdall.ui.settings.security

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_security_settings.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.dialogs.fingerprint.FingerprintDialog
import pm.gnosis.heimdall.ui.settings.security.changepassword.ChangePasswordActivity
import pm.gnosis.heimdall.ui.settings.security.revealmnemonic.RevealMnemonicActivity
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import timber.log.Timber
import javax.inject.Inject

class SecuritySettingsActivity : BaseActivity() {
    override fun screenId() = ScreenId.SECURITY_SETTINGS

    @Inject
    lateinit var viewModel: SecuritySettingsContract

    private val removeFingerprintClick = PublishSubject.create<Unit>()
    private val getFingerprintStateSubject = PublishSubject.create<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_security_settings)
        setupToolbar(layout_security_settings_toolbar)
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.isFingerprintAvailable()) setupFingerprintAction()

        disposables += layout_security_settings_show_mnemonic.clicks()
            .subscribeBy(onNext = {
                startActivity(RevealMnemonicActivity.createIntent(this))
            }, onError = Timber::e)

        disposables += layout_security_settings_change_password.clicks()
            .subscribeBy(onNext = {
                startActivity(ChangePasswordActivity.createIntent(this))
            }, onError = Timber::e)
    }

    private fun setupFingerprintAction() {
        layout_security_settings_fingerprint_switch_container.visibility = View.VISIBLE
        disposables += layout_security_settings_fingerprint_switch_container.clicks()
            .subscribeBy(onNext = {
                if (layout_security_settings_switch.isChecked) {
                    removeFingerprintClick.onNext(Unit)
                } else {
                    showDialog()
                }
            }, onError = Timber::e)

        disposables += removeFingerprintClick
            .flatMapSingle { viewModel.clearFingerprintData() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                layout_security_settings_switch.isChecked = false
                toast(R.string.fingerprint_unlock_disabled)
            }, onError = Timber::e)

        disposables += getFingerprintStateSubject.flatMapSingle { encryptionManager.isFingerPrintSet() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = {
                layout_security_settings_switch.isChecked = it
            }, onError = Timber::e)
        getFingerprintStateSubject.onNext(Unit)
    }

    private fun showDialog() {
        val dialog = FingerprintDialog.create()
        dialog.show(supportFragmentManager, null)
        dialog.dismissListener = { getFingerprintStateSubject.onNext(Unit) }
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, SecuritySettingsActivity::class.java)
    }
}
