package pm.gnosis.heimdall.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_onboarding_intro.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.onboarding.account.AccountSetupActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import timber.log.Timber

class OnboardingIntro : BaseActivity() {

    override fun screenId() = ScreenId.ONBOARDING_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_onboarding_intro)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_onboarding_intro_get_started.clicks()
                .subscribeBy(
                        onNext = { startActivity(PasswordSetupActivity.createIntent(this)) },
                        onError = Timber::e)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, OnboardingIntro::class.java)
    }
}
