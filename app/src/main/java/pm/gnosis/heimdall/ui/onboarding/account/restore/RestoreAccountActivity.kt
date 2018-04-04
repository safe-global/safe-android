package pm.gnosis.heimdall.ui.onboarding.account.restore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_restore_account.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.SecuredBaseActivity
import pm.gnosis.heimdall.ui.onboarding.SetupSafeIntroActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.svalinn.common.utils.startActivity
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.utils.trimWhitespace
import timber.log.Timber
import javax.inject.Inject

class RestoreAccountActivity : SecuredBaseActivity() {

    override fun screenId() = ScreenId.RESTORE_ACCOUNT

    @Inject
    lateinit var viewModel: RestoreAccountContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_restore_account)

        layout_restore_account_mnemonic.disableAccessibility()
    }

    override fun onWindowObscured() {
        super.onWindowObscured()
        // Window is obscured, clear input and disable to prevent potential leak
        layout_restore_account_mnemonic.text = null
        layout_restore_account_mnemonic.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        onSavingAccount(false)
        disposables += layout_restore_account_restore.clicks()
            .map { layout_restore_account_mnemonic.text.toString().trimWhitespace() }
            .flatMapSingle {
                viewModel.saveAccountWithMnemonic(it)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { onSavingAccount(isSaving = true) }
                    .doAfterTerminate { onSavingAccount(isSaving = false) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = { onAccountSaved() }, onError = ::onAccountSaveError)

        disposables += layout_restore_account_back.clicks()
            .subscribeBy(onNext = { finish() }, onError = Timber::e)
    }

    private fun onAccountSaved() {
        startActivity(SetupSafeIntroActivity.createIntent(this), clearStack = false)
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
