package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
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
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupActivity
import pm.gnosis.heimdall.ui.safe.overview.SafesOverviewActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.heimdall.utils.setColorFilterCompat
import pm.gnosis.heimdall.utils.setSelectedCompoundDrawablesWithIntrinsicBounds
import pm.gnosis.svalinn.common.utils.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
                    layout_password_confirm_confirm.performClick()
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_password_confirm_confirm.clicks()
            .flatMapSingle {
                viewModel.createAccount(passwordHash, layout_password_confirm_password.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { onCreateAccountLoading(true) }
                    .doFinally { onCreateAccountLoading(false) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onCreateAccount, onError = ::onPasswordSetupError)

        disposables += layout_password_confirm_back.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_password_confirm_password.textChanges()
            .doOnNext { enableConfirm(false) }
            .debounce(500, TimeUnit.MILLISECONDS)
            .flatMapSingle { viewModel.isSamePassword(passwordHash, it.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onIsSamePassword, onError = Timber::e)
    }

    private fun onIsSamePassword(isSamePassword: Boolean) {
        layout_password_confirm_info.visible(!isSamePassword)
        layout_password_confirm_password.setSelectedCompoundDrawablesWithIntrinsicBounds(
            right = ContextCompat.getDrawable(this, if (isSamePassword) R.drawable.ic_green_check else R.drawable.ic_error)
        )
        enableConfirm(isSamePassword)
    }

    private fun onCreateAccount(ignored: Unit) {
        startActivity(
            if (viewModel.canSetupFingerprint()) FingerprintSetupActivity.createIntent(this)
            else SafesOverviewActivity.createIntent(this),
            clearStack = true
        )
    }

    private fun onPasswordSetupError(throwable: Throwable) {
        Timber.e(throwable)
        (throwable as? PasswordInvalidException)?.let { passwordInvalidException ->
            when (passwordInvalidException.reason) {
                is PasswordNotLongEnough -> snackbar(layout_password_confirm_coordinator, R.string.password_too_short)
                is PasswordsNotEqual -> snackbar(layout_password_confirm_coordinator, R.string.passwords_do_not_match)
            }
        }
    }

    private fun onCreateAccountLoading(isLoading: Boolean) {
        layout_password_confirm_progress.visible(isLoading)
        enableConfirm(false)
    }

    private fun enableConfirm(enable: Boolean) {
        layout_password_confirm_confirm.isEnabled = enable
        layout_password_confirm_bottom_container.setBackgroundColor(getColorCompat(if (enable) R.color.blue_button_new else R.color.gray_bottom_bar))
        layout_password_confirm_text.setTextColor(getColorCompat(if (enable) R.color.white else R.color.text_light_2_new))
        layout_password_confirm_back.setColorFilterCompat(if (enable) R.color.white else R.color.disabled)
        layout_password_confirm_next_arrow.setColorFilterCompat(if (enable) R.color.white else R.color.disabled)
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
