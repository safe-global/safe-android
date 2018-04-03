package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_password_setup.*
import pm.gnosis.crypto.utils.HashUtils
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.SecuredBaseActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import timber.log.Timber
import javax.inject.Inject

class PasswordSetupActivity : SecuredBaseActivity() {

    override fun screenId() = ScreenId.PASSWORD_SETUP

    @Inject
    lateinit var viewModel: PasswordSetupContract

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_password_setup)

        layout_password_setup_password.disableAccessibility()
    }

    override fun onWindowObscured() {
        super.onWindowObscured()
        // Window is obscured, clear input and disable to prevent potential leak
        layout_password_setup_password.text = null
        layout_password_setup_password.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_password_setup_next.clicks()
            .map { viewModel.isPasswordValid(layout_password_setup_password.text.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onPasswordValidation, onError = Timber::e)

        disposables += layout_password_setup_back.clicks()
            .subscribeBy(onNext = { finish() }, onError = Timber::e)

        disposables += layout_password_setup_password.textChanges()
            .subscribeBy(onNext = {
                layout_password_setup_input_layout.error = null
            }, onError = Timber::e)
    }

    private fun onPasswordValidation(passwordValidation: PasswordValidation) {
        when (passwordValidation) {
            is PasswordValid -> startActivity(PasswordConfirmActivity.createIntent(this, Sha3Utils.keccak(passwordValidation.password.toByteArray())))
            is PasswordNotLongEnough -> layout_password_setup_input_layout.error = getString(R.string.password_too_short)
        }
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build()
            .inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, PasswordSetupActivity::class.java)
    }
}
