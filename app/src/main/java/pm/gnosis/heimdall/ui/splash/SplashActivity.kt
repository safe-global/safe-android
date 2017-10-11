package pm.gnosis.heimdall.ui.splash

import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.startActivity
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.main.MainActivity
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicActivity
import timber.log.Timber
import javax.inject.Inject

class SplashActivity : BaseActivity() {
    @Inject lateinit var viewModel: SplashViewModel

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
                .subscribeBy(onSuccess = this::handleAction, onError = this::onError)
    }

    private fun handleAction(action: ViewAction) {
        when (action) {
            is StartMain -> startMain()
            is StartSetup -> startSetup()
        }
    }

    private fun startMain() {
        startActivity(MainActivity.createIntent(this), noHistory = true)
    }

    private fun startSetup() {
        startActivity(GenerateMnemonicActivity.createIntent(this), noHistory = true)
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
