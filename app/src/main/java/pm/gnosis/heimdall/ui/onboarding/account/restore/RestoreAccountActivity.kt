package pm.gnosis.heimdall.ui.onboarding.account.restore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_restore_account.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.startActivity
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.utils.trimWhitespace
import pm.gnosis.utils.words
import timber.log.Timber
import javax.inject.Inject

class RestoreAccountActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: RestoreAccountContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_restore_account)
        registerToolbar(layout_restore_account_toolbar)
    }

    override fun onStart() {
        super.onStart()
        onSavingAccount(false)
        disposables += mnemonicValidatorDisposable()
        disposables += mnemonicChangesDisposable()
    }

    private fun mnemonicValidatorDisposable() =
            layout_restore_account_restore.clicks()
                    .map { layout_restore_account_mnemonic.text.toString().trimWhitespace() }
                    .flatMap {
                        viewModel.saveAccountWithMnemonic(it)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnSubscribe { onSavingAccount(isSaving = true) }
                                .doAfterTerminate { onSavingAccount(isSaving = false) }
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeForResult(::onAccountSaved, ::onAccountSaveError)

    private fun mnemonicChangesDisposable() =
            layout_restore_account_mnemonic.textChanges()
                    .map { it.toString() }
                    .subscribeBy(
                            onNext = {
                                updateWordCounter(it)
                            },
                            onError = Timber::e)

    private fun updateWordCounter(mnemonic: String) {
        val text = "${mnemonic.words().count()}/12 words"
        layout_restore_account_word_counter.text = text
    }

    private fun onAccountSaved(intent: Intent) {
        startActivity(intent, noHistory = false)
    }

    private fun onAccountSaveError(throwable: Throwable) {
        errorSnackbar(layout_restore_account_coordinator, throwable)
    }

    private fun onSavingAccount(isSaving: Boolean) {
        layout_restore_account_restore.isEnabled = !isSaving
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, RestoreAccountActivity::class.java)
    }
}
