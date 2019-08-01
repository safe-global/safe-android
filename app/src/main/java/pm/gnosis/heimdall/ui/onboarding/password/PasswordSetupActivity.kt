package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_password_setup.*
import kotlinx.android.synthetic.main.layout_two_step_panel.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.PasswordHelper
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.colorStatusBar
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.subscribeForResult
import timber.log.Timber
import java.util.concurrent.TimeUnit


class PasswordSetupActivity : ViewModelActivity<PasswordSetupContract>() {

    override fun screenId() = ScreenId.PASSWORD

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        window.colorStatusBar(R.color.safe_green)
        super.onCreate(savedInstanceState)

        layout_password_setup_password.disableAccessibility()
        layout_password_setup_password.requestFocus()
        layout_password_setup_password.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE, EditorInfo.IME_NULL ->
                    layout_password_setup_bottom_container.forward.performClick()
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_password_setup_bottom_container.forwardClicks
            // We validate again the password on each click
            .switchMapSingle { viewModel.validatePassword(layout_password_setup_password.text.toString()) }
            .filter { it is DataResult && it.data.all { it.valid } }
            .flatMapSingle { viewModel.passwordToHash(layout_password_setup_password.text.toString()) }
            .subscribeForResult(onNext = { startActivity(PasswordConfirmActivity.createIntent(this, it)) }, onError = Timber::e)

        disposables += layout_password_setup_password.textChanges()
            .doOnNext { enableNext(false) }
            .debounce(500, TimeUnit.MILLISECONDS)
            .switchMapSingle { viewModel.validatePassword(it.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onPasswordValidation, onError = Timber::e)
    }

    private fun onPasswordValidation(validationConditions: Collection<PasswordValidationCondition>) {
        val (_, validPassword) =
            PasswordHelper.Handler.applyToView(layout_password_setup_password, layout_password_setup_validation_info, validationConditions)
        enableNext(validPassword)
    }

    private fun enableNext(enable: Boolean) {
        layout_password_setup_bottom_container.setForwardEnabled(enable)
    }

    override fun layout() = R.layout.layout_password_setup

    override fun inject(component: ViewComponent) = viewComponent().inject(this)

    companion object {
        fun createIntent(context: Context) = Intent(context, PasswordSetupActivity::class.java)
    }
}
