package pm.gnosis.heimdall.ui.splash

import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.onboarding.OnboardingIntro
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.svalinn.common.utils.startActivity
import timber.log.Timber
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    override fun screenId() = ScreenId.SPLASH

    @Inject
    lateinit var viewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.initialSetup()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::handleAction, onError = ::onError)
    }

    private fun handleAction(action: ViewAction) {
        when (action) {
            is StartMain -> startMain()
            is StartPasswordSetup -> startPasswordSetup()
        }
    }

    private fun startMain() {
        startActivity(SafeMainActivity.createIntent(this), clearStack = true)
    }

    private fun startPasswordSetup() {
        startActivity(OnboardingIntro.createIntent(this), clearStack = true)
    }

    private fun onError(throwable: Throwable) {
        Timber.e(throwable)
        startMain()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }
}
