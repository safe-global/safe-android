package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_password_setup.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.SecuredBaseActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.heimdall.utils.setColorFilterCompat
import pm.gnosis.heimdall.utils.setSelectedCompoundDrawablesWithIntrinsicBounds
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.subscribeForResult
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
        layout_password_setup_password.requestFocus()
        layout_password_setup_password.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE, EditorInfo.IME_NULL ->
                    layout_password_setup_next.performClick()
            }
            true
        }
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
            .flatMapSingle { viewModel.passwordToHash(layout_password_setup_password.text.toString()) }
            .subscribeForResult(onNext = { startActivity(PasswordConfirmActivity.createIntent(this, it)) }, onError = Timber::e)

        disposables += layout_password_setup_password.textChanges()
            .doOnNext { enableNext(false) }
            .debounce(500, TimeUnit.MILLISECONDS)
            .switchMapSingle { viewModel.validatePassword(it.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onPasswordValidation, onError = Timber::e)
    }

    private fun onPasswordValidation(validationConditions: List<Pair<PasswordValidationCondition, Boolean>>) {
        layout_password_setup_password.setSelectedCompoundDrawablesWithIntrinsicBounds(
            right = ContextCompat.getDrawable(this, if (validationConditions.all { it.second }) R.drawable.ic_green_check else R.drawable.ic_error)
        )
        layout_password_setup_validation_info.text = ""
        validationConditions.forEach {
            when (it.first) {
                PasswordValidationCondition.NON_IDENTICAL_CHARACTERS -> layout_password_setup_validation_info.append(
                    getSpannableString(
                        getString(R.string.password_validation_identical_characters),
                        it.second
                    )
                )
                PasswordValidationCondition.MINIMUM_CHARACTERS -> layout_password_setup_validation_info.append(
                    getSpannableString(
                        getString(R.string.password_validation_minimum_characters),
                        it.second
                    )
                )
                PasswordValidationCondition.ONE_NUMBER_ONE_LETTER -> layout_password_setup_validation_info.append(
                    getSpannableString(
                        getString(R.string.password_validation_one_number_one_letter),
                        it.second
                    )
                )
            }
        }

        enableNext(validationConditions.all { it.second })
    }

    private fun getSpannableString(message: String, condition: Boolean) =
        SpannableString(message).apply {
            setSpan(
                ForegroundColorSpan(if (condition) getColorCompat(R.color.green_new) else getColorCompat(R.color.red_new)),
                0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    private fun enableNext(enable: Boolean) {
        layout_password_setup_next.isEnabled = enable
        layout_password_setup_bottom_container.setBackgroundColor(getColorCompat(if (enable) R.color.blue_button_new else R.color.gray_bottom_bar))
        layout_password_setup_next_text.setTextColor(getColorCompat(if (enable) R.color.white else R.color.text_light_2_new))
        layout_password_confirm_next_arrow.setColorFilterCompat(if (enable) R.color.white else R.color.disabled)
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
