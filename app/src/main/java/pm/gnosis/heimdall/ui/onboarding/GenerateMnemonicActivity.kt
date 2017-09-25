package pm.gnosis.heimdall.ui.onboarding

import android.content.Intent
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_generate_mnemonic.*
import pm.gnosis.heimdall.GnosisAuthenticatorApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.ui.MainActivity
import pm.gnosis.heimdall.ui.base.BaseActivity
import timber.log.Timber
import javax.inject.Inject

class GenerateMnemonicActivity : BaseActivity() {
    @Inject lateinit var presenter: GenerateMnemonicPresenter

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

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisAuthenticatorApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
