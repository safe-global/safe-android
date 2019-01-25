package pm.gnosis.heimdall.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.text.Html
import android.text.method.LinkMovementMethod
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.bottom_sheet_terms_and_conditions.*
import kotlinx.android.synthetic.main.layout_onboarding_intro.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import timber.log.Timber

class OnboardingIntroActivity : BaseActivity() {
    override fun screenId() = ScreenId.WELCOME

    private lateinit var termsBottomSheetDialog: TermsBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_onboarding_intro)

        termsBottomSheetDialog = TermsBottomSheetDialog(this).apply {
            setContentView(layoutInflater.inflate(R.layout.bottom_sheet_terms_and_conditions, null))
        }

        termsBottomSheetDialog.bottom_sheet_terms_and_conditions_description.apply {
            text = Html.fromHtml(getString(R.string.terms_info))
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_onboarding_intro_get_started.clicks()
            .subscribeBy(
                onNext = {
                    termsBottomSheetDialog.show()
                    eventTracker.submit(Event.ScreenView(ScreenId.WELCOME_TERMS))
                }, onError = Timber::e
            )

        disposables += termsBottomSheetDialog.bottom_sheet_terms_and_conditions_agree.clicks()
            .subscribeBy(onNext = {
                termsBottomSheetDialog.hide()
                startActivity(PasswordSetupActivity.createIntent(this))
            }, onError = Timber::e)

        disposables += termsBottomSheetDialog.bottom_sheet_terms_and_conditions_reject.clicks()
            .subscribeBy(onNext = { termsBottomSheetDialog.hide() }, onError = Timber::e)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, OnboardingIntroActivity::class.java)
    }

    class TermsBottomSheetDialog(context: Context) : BottomSheetDialog(context)
}
