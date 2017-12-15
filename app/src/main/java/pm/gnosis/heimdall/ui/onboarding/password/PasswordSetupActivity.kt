package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.layout_password_setup.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.startActivity
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.onboarding.OnboardingIntro
import pm.gnosis.heimdall.ui.onboarding.account.AccountSetupActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import timber.log.Timber
import javax.inject.Inject

class PasswordSetupActivity : BaseActivity() {

    override fun screenId() = ScreenId.PASSWORD_SETUP

    @Inject
    lateinit var viewModel: PasswordSetupContract

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_password_setup)
    }

    override fun onStart() {
        super.onStart()
        layout_password_setup_next.clicks()
                .flatMap {
                    viewModel.setPassword(
                            layout_password_setup_password.text.toString(),
                            layout_password_setup_confirmation.text.toString())
                }
                .subscribeForResult(onNext = { onPasswordSet() }, onError = ::onPasswordSetError)
    }

    private fun onPasswordSet() {
        startActivity(AccountSetupActivity.createIntent(this), noHistory = true)
    }

    private fun onPasswordSetError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_password_setup_coordinator, throwable)
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
