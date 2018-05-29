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
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.heimdall.utils.setColorFilterCompat
import pm.gnosis.heimdall.utils.setCompoundDrawables
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.subscribeForResult
import timber.log.Timber
import java.util.concurrent.TimeUnit

class PasswordSetupActivity : ViewModelActivity<PasswordSetupContract>() {

    override fun screenId() = ScreenId.PASSWORD_SETUP

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)

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
        layout_password_setup_password.setCompoundDrawables(
            right =
            if (layout_password_setup_password.text.isEmpty()) null
            else ContextCompat.getDrawable(this, if (validationConditions.all { it.second }) R.drawable.ic_green_check else R.drawable.ic_error)
        )
        layout_password_setup_validation_info.text = ""
        validationConditions.forEach {
            layout_password_setup_validation_info.append(
                getSpannableString(
                    when (it.first) {
                        PasswordValidationCondition.NON_IDENTICAL_CHARACTERS -> getString(R.string.password_validation_identical_characters)
                        PasswordValidationCondition.MINIMUM_CHARACTERS -> getString(R.string.password_validation_minimum_characters)
                        PasswordValidationCondition.ONE_NUMBER_ONE_LETTER -> getString(R.string.password_validation_one_number_one_letter)
                    },
                    it.second
                )
            )
        }

        enableNext(validationConditions.all { it.second })
    }

    private fun getSpannableString(message: String, condition: Boolean): SpannableString {
        val color = when {
            layout_password_setup_password.text.isEmpty() -> getColorCompat(R.color.battleship_grey)
            condition -> getColorCompat(R.color.green_teal)
            else -> getColorCompat(R.color.tomato)
        }
        return SpannableString(message).apply {
            setSpan(ForegroundColorSpan(color), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun enableNext(enable: Boolean) {
        layout_password_setup_next.isEnabled = enable
        layout_password_setup_bottom_container.setBackgroundColor(getColorCompat(if (enable) R.color.azure else R.color.pale_grey))
        layout_password_setup_next_text.setTextColor(getColorCompat(if (enable) R.color.white else R.color.bluey_grey))
        layout_password_confirm_next_arrow.setColorFilterCompat(if (enable) R.color.white else R.color.bluey_grey)
    }

    override fun layout() = R.layout.layout_password_setup

    override fun inject(component: ViewComponent) = viewComponent().inject(this)


    companion object {
        fun createIntent(context: Context) = Intent(context, PasswordSetupActivity::class.java)
    }
}
