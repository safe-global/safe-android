package pm.gnosis.heimdall.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Html
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_generate_mnemonic.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.startActivity
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.main.MainActivity
import timber.log.Timber
import javax.inject.Inject

class GenerateMnemonicActivity : BaseActivity() {
    @Inject lateinit var presenter: GenerateMnemonicPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_generate_mnemonic)
    }

    override fun onStart() {
        super.onStart()
        disposables += generateMnemonicDisposable()
        disposables += saveAccountConfirmationDisposable()

        layout_generate_mnemonic_regenerate_button.callOnClick()
    }

    private fun generateMnemonicDisposable() =
            layout_generate_mnemonic_regenerate_button.clicks()
                    .flatMap { presenter.generateMnemonic() }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onNext = this::onMnemonic, onError = this::onMnemonicError)

    private fun onMnemonic(mnemonic: String) {
        layout_generate_mnemonic_mnemonic.text = mnemonic
    }

    private fun onMnemonicError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun saveAccountConfirmationDisposable() =
            layout_generate_mnemonic_save.clicks()
                    .subscribeBy(onNext = { showConfirmationDialog(layout_generate_mnemonic_mnemonic.text.toString()) },
                            onError = Timber::e)


    private fun showConfirmationDialog(mnemonic: String) {
        AlertDialog.Builder(this)
                .setPositiveButton(getString(R.string.yes), { _, _ -> disposables += onMnemonicConfirmedDisposable() })
                .setNegativeButton(getString(R.string.no), { _, _ -> })
                .setTitle(getString(R.string.dialog_title_save_mnemonic))
                .setMessage(Html.fromHtml(resources.getString(R.string.generate_mnemonic_activity_dialog, mnemonic)))
                .show()
    }

    private fun onMnemonicConfirmedDisposable() =
            presenter.saveAccountWithMnemonic(layout_generate_mnemonic_mnemonic.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onSavedAccountWithMnemonic,
                            onError = this::onSavedAccountWithMnemonicWithError)


    private fun onSavedAccountWithMnemonic() {
        startActivity(MainActivity.createIntent(this), noHistory = true)
    }

    private fun onSavedAccountWithMnemonicWithError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, GenerateMnemonicActivity::class.java)
    }
}
