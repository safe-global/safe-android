package pm.gnosis.heimdall.ui.onboarding.account.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Html
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_generate_mnemonic.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.SecuredBaseActivity
import pm.gnosis.heimdall.ui.onboarding.SetupSafeIntroActivity
import pm.gnosis.heimdall.ui.onboarding.account.restore.RestoreAccountActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.svalinn.common.utils.startActivity
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class GenerateMnemonicActivity : SecuredBaseActivity() {

    override fun screenId() = ScreenId.GENERATE_MNEMONIC

    @Inject
    lateinit var viewModel: GenerateMnemonicContract

    private var mnemonicGeneratorDisposable: Disposable? = null
    private val confirmDialogClick = PublishSubject.create<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_generate_mnemonic)

        layout_generate_mnemonic_mnemonic.disableAccessibility()
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_generate_mnemonic_save.clicks()
            .subscribeBy(onNext = { showConfirmationDialog(layout_generate_mnemonic_mnemonic.text.toString()) }, onError = Timber::e)

        disposables += layout_generate_mnemonic_reveal.clicks()
            .flatMapSingle { viewModel.generateMnemonic() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onMnemonic, onError = ::onMnemonicError)

        disposables += confirmDialogClick
            .flatMapSingle {
                viewModel.saveAccountWithMnemonic(it)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { layout_generate_mnemonic_progress_bar.visibility = View.VISIBLE }
                    .doOnEvent { _, _ -> layout_generate_mnemonic_progress_bar.visibility = View.GONE }
            }
            .subscribeForResult(onNext = { onAccountSaved() }, onError = ::onAccountSaveError)

        disposables += layout_generate_mnemonic_restore.clicks()
            .subscribeBy(onNext = { startActivity(RestoreAccountActivity.createIntent(this)) }, onError = Timber::e)
    }

    private fun onAccountSaved() {
        startActivity(SetupSafeIntroActivity.createIntent(this), clearStack = true)
    }

    private fun onAccountSaveError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onMnemonic(mnemonic: String) {
        layout_generate_mnemonic_reveal.visible(false)
        layout_generate_mnemonic_reveal_container.visible(true)
        layout_generate_mnemonic_mnemonic.text = mnemonic
    }

    private fun onMnemonicError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun showConfirmationDialog(mnemonic: String) {
        AlertDialog.Builder(this)
            .setPositiveButton(getString(R.string.yes), { _, _ -> confirmDialogClick.onNext(layout_generate_mnemonic_mnemonic.text.toString()) })
            .setNegativeButton(getString(R.string.no), { _, _ -> })
            .setTitle(getString(R.string.dialog_title_save_mnemonic))
            .setMessage(Html.fromHtml(resources.getString(R.string.generate_mnemonic_activity_dialog, mnemonic)))
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mnemonicGeneratorDisposable?.dispose()
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
