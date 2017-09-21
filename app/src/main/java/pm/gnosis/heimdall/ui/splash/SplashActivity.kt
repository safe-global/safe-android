package pm.gnosis.heimdall.ui.splash

import android.arch.persistence.room.EmptyResultSetException
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.startActivity
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.main.MainActivity
import pm.gnosis.heimdall.ui.onboarding.OnBoardingActivity
import javax.inject.Inject

class SplashActivity : BaseActivity() {
    @Inject lateinit var presenter: SplashPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_splash)
        inject()
    }

    override fun onStart() {
        super.onStart()
        disposables += presenter.initialSetup().andThen(presenter.loadActiveAccount())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = { startApplication() }, onError = this::onError)
    }

    private fun startApplication() {
        startActivity(MainActivity::class, noHistory = true)
    }

    private fun onError(throwable: Throwable) {
        //TODO: when refactoring the model of the application add common exception for NoAccount
        if (throwable is EmptyResultSetException || throwable is NoSuchElementException) {
            startActivity(OnBoardingActivity::class, noHistory = true)
        } else {
            startApplication()
        }
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
