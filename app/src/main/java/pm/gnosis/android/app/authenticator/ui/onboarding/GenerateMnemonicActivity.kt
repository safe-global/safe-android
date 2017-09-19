package pm.gnosis.android.app.authenticator.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_generate_mnemonic.*
import pm.gnosis.android.app.authenticator.GnosisAuthenticatorApplication
import pm.gnosis.android.app.authenticator.R
import pm.gnosis.android.app.authenticator.di.component.DaggerViewComponent
import pm.gnosis.android.app.authenticator.di.module.ViewModule
import pm.gnosis.android.app.authenticator.ui.MainActivity
import timber.log.Timber
import javax.inject.Inject

class GenerateMnemonicActivity : AppCompatActivity() {
    @Inject lateinit var presenter: GenerateMnemonicPresenter

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_generate_mnemonic)

        activity_generate_mnemonic_regenerate_button.setOnClickListener {
            disposables += generateMnemonicDisposable()
        }

        activity_generate_mnemonic_save.setOnClickListener {
            disposables += saveAccountWithMnemonicDisposable(activity_generate_mnemonic_mnemonic.text.toString())
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += generateMnemonicDisposable()
    }

    private fun generateMnemonicDisposable() =
            presenter.generateMnemonic()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onNext = this::onMnemonic, onError = this::onMnemonicError)

    private fun onMnemonic(mnemonic: String) {
        activity_generate_mnemonic_mnemonic.text = mnemonic
    }

    private fun onMnemonicError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun saveAccountWithMnemonicDisposable(mnemonic: String) =
            presenter.saveAccountWithMnemonic(mnemonic)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onSavedAccountWithMnemonic,
                            onError = this::onSavedAccountWithMnemonicWithError)

    private fun onSavedAccountWithMnemonic() {
        val i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(i)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun onSavedAccountWithMnemonicWithError(throwable: Throwable) {
        Timber.e(throwable)
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
