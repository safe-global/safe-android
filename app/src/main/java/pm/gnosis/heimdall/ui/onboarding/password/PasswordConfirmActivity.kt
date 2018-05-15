package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_password_confirm.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.SecuredBaseActivity
import pm.gnosis.heimdall.ui.onboarding.account.AccountSetupActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.svalinn.common.utils.startActivity
import pm.gnosis.svalinn.common.utils.subscribeForResult
import timber.log.Timber
import javax.inject.Inject

class PasswordConfirmActivity : SecuredBaseActivity() {
    override fun screenId() = ScreenId.PASSWORD_CONFIRM

    @Inject
    lateinit var viewModel: PasswordSetupContract

    private lateinit var passwordHash: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_password_confirm)

        intent.getByteArrayExtra(EXTRA_PASSWORD_HASH)?.let { passwordHash = it } ?: run {
            Timber.e("PasswordConfirmActivity: Password is null")
            finish()
        }

        layout_password_confirm_password.disableAccessibility()
        layout_password_confirm_password.requestFocus()
        layout_password_confirm_password.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE, EditorInfo.IME_NULL ->
                    layout_password_confirm_next.performClick()
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_password_confirm_next.clicks()
            .flatMapSingle { viewModel.setPassword(passwordHash, layout_password_confirm_password.text.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onPasswordSetup, onError = ::onPasswordSetupError)

        disposables += layout_password_confirm_back.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_password_confirm_password.textChanges()
            .subscribeBy(onNext = { layout_password_confirm_input_layout.error = null }, onError = Timber::e)
    }

    private fun onPasswordSetup(ignored: Unit) {
        startActivity(AccountSetupActivity.createIntent(this), clearStack = true)
    }

    private fun onPasswordSetupError(throwable: Throwable) {
        Timber.e(throwable)
        (throwable as? PasswordInvalidException)?.let { passwordInvalidException ->
            when (passwordInvalidException.reason) {
                is PasswordNotLongEnough -> layout_password_confirm_input_layout.error = getString(R.string.password_too_short)
                is PasswordsNotEqual -> layout_password_confirm_input_layout.error = getString(R.string.passwords_do_not_match)
            }
        }
    }

    override fun onWindowObscured() {
        super.onWindowObscured()
        // Window is obscured, clear input and disable to prevent potential leak
        layout_password_confirm_password.text = null
        layout_password_confirm_password.isEnabled = false
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        private const val EXTRA_PASSWORD_HASH = "extra.bytearray.passwordHash"

        fun createIntent(context: Context, passwordHash: ByteArray) = Intent(context, PasswordConfirmActivity::class.java).apply {
            putExtra(EXTRA_PASSWORD_HASH, passwordHash)
        }
    }
}
