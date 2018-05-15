package pm.gnosis.heimdall.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.bottom_sheet_terms_and_conditions.*
import kotlinx.android.synthetic.main.layout_onboarding_intro.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import timber.log.Timber

class OnboardingIntro : BaseActivity() {
    override fun screenId() = ScreenId.ONBOARDING_INTRO

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_onboarding_intro)
        bottomSheetBehavior = BottomSheetBehavior.from(layout_onboarding_intro_bottom_sheet).apply { state = BottomSheetBehavior.STATE_HIDDEN }
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_onboarding_intro_get_started.clicks()
            .subscribeBy(
                onNext = {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }, onError = Timber::e
            )

        disposables += bottom_sheet_terms_and_conditions_agree.clicks()
            .subscribeBy(onNext = {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                startActivity(PasswordSetupActivity.createIntent(this))
            }, onError = Timber::e)

        disposables += bottom_sheet_terms_and_conditions_reject.clicks()
            .subscribeBy(onNext = { bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }, onError = Timber::e)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, OnboardingIntro::class.java)
    }
}
