package pm.gnosis.android.app.authenticator.ui.onboarding

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_generate_mnemonic.*
import okio.ByteString
import pm.gnosis.android.app.authenticator.GnosisAuthenticatorApplication
import pm.gnosis.android.app.authenticator.R
import pm.gnosis.android.app.authenticator.di.component.DaggerViewComponent
import pm.gnosis.android.app.authenticator.di.module.ViewModule
import pm.gnosis.android.app.authenticator.util.toast
import pm.gnosis.crypto.KeyGenerator
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.utils.toHex
import pm.gnosis.utils.toHexString
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
        toast(Bip39.mnemonicToSeedHex(mnemonic))

        val hdNode = KeyGenerator().masterNode(ByteString.of(*Bip39.mnemonicToSeed(mnemonic)))
        Timber.d(hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM).deriveChild(0).keyPair.address.toHexString())
    }

    private fun onMnemonicError(throwable: Throwable) {
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
