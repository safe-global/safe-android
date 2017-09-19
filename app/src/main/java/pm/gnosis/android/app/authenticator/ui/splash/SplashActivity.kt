package pm.gnosis.android.app.authenticator.ui.splash

import android.arch.persistence.room.EmptyResultSetException
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toSingle
import pm.gnosis.android.app.authenticator.GnosisAuthenticatorApplication
import pm.gnosis.android.app.authenticator.R
import pm.gnosis.android.app.authenticator.di.component.DaggerViewComponent
import pm.gnosis.android.app.authenticator.di.module.ViewModule
import pm.gnosis.android.app.authenticator.ui.MainActivity
import pm.gnosis.android.app.authenticator.ui.onboarding.GenerateMnemonicActivity
import javax.inject.Inject
import kotlin.reflect.KClass

class SplashActivity : AppCompatActivity() {
    @Inject lateinit var presenter: SplashPresenter

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        inject()
    }

    override fun onStart() {
        super.onStart()
        disposables += presenter.initialSetup().toSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { presenter.loadActiveAccount() }
                .subscribeBy(onSuccess = { startApplication() }, onError = this::onError)
    }

    private fun startApplication() {
        startActivityWithNoHistory(MainActivity::class)
    }

    private fun onError(throwable: Throwable) {
        if (throwable is EmptyResultSetException) {
            startActivityWithNoHistory(GenerateMnemonicActivity::class)
        } else {
            startApplication()
        }
    }

    //TODO: extract util
    private fun <T : AppCompatActivity> startActivityWithNoHistory(activity: KClass<T>) {
        val i = Intent(this, activity.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(i)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisAuthenticatorApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
