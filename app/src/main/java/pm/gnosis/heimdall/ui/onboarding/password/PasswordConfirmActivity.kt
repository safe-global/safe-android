package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.editorActions
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_password_confirm.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.heimdall.utils.setColorFilterCompat
import pm.gnosis.heimdall.utils.setCompoundDrawables
import pm.gnosis.svalinn.common.utils.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PasswordConfirmActivity : ViewModelActivity<PasswordSetupContract>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    private lateinit var passwordHash: ByteArray

    override fun screenId() = ScreenId.PASSWORD_CONFIRM

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)

        intent.getByteArrayExtra(EXTRA_PASSWORD_HASH)?.let { passwordHash = it } ?: run {
            Timber.e("PasswordConfirmActivity: Password is null")
            finish()
        }

        layout_password_confirm_password.disableAccessibility()
        layout_password_confirm_password.requestFocus()
        enableConfirm(false)
    }

    override fun onStart() {
        super.onStart()
        disposables += Observable.merge(
            layout_password_confirm_confirm.clicks(),
            layout_password_confirm_password.editorActions()
                .filter { it == EditorInfo.IME_ACTION_DONE || it == EditorInfo.IME_NULL }
                .map { Unit }
        )
            .flatMapSingle {
                viewModel.createAccount(passwordHash, layout_password_confirm_password.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { _ -> onCreateAccountLoading(true) }
                    .doFinally { onCreateAccountLoading(false) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onCreateAccount, onError = ::onPasswordSetupError)

        disposables += layout_password_confirm_back.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_password_confirm_password.textChanges()
            .skipInitialValue()
            .doOnNext { enableConfirm(false) }
            .debounce(500, TimeUnit.MILLISECONDS)
            .flatMapSingle { viewModel.isSamePassword(passwordHash, it.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onIsSamePassword, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_password_confirm_toolbar_shadow, layout_password_confirm_content_scroll)
    }

    private fun onIsSamePassword(isSamePassword: Boolean) {
        layout_password_confirm_info.visible(!isSamePassword)
        layout_password_confirm_password.setCompoundDrawables(
            right = ContextCompat.getDrawable(this, if (isSamePassword) R.drawable.ic_green_check else R.drawable.ic_error)
        )
        enableConfirm(isSamePassword)
    }

    private fun onCreateAccount(intent: Intent) {
        startActivity(intent, clearStack = true)
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
        layout_password_confirm_bottom_container.setBackgroundColor(getColorCompat(if (enable) R.color.azure else R.color.pale_grey))
        layout_password_confirm_text.setTextColor(getColorCompat(if (enable) R.color.white else R.color.bluey_grey))
        layout_password_confirm_back.setColorFilterCompat(if (enable) R.color.white else R.color.bluey_grey)
        layout_password_confirm_next_arrow.setColorFilterCompat(if (enable) R.color.white else R.color.bluey_grey)
    }

    override fun layout() = R.layout.layout_password_confirm

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        private const val EXTRA_PASSWORD_HASH = "extra.bytearray.passwordHash"

        fun createIntent(context: Context, passwordHash: ByteArray) = Intent(context, PasswordConfirmActivity::class.java).apply {
            putExtra(EXTRA_PASSWORD_HASH, passwordHash)
        }
    }
}
