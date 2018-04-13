package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Html
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_safe_recovery_phrase.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class SafeRecoveryPhraseActivity : BaseActivity() {
    override fun screenId() = ScreenId.SAFE_RECOVERY_PHRASE

    @Inject
    lateinit var viewModel: SafeRecoveryPhraseContract

    private lateinit var chromeExtension: Solidity.Address

    private val confirmDialogClick = PublishSubject.create<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chromeExtension = intent.getStringExtra(CHROME_EXTENSION_ADDRESS_EXTRA).asEthereumAddress() ?: run { finish(); return }

        inject()
        setContentView(R.layout.layout_safe_recovery_phrase)
    }

    override fun onStart() {
        super.onStart()

        disposables += viewModel.generateMnemonic()
            .observeOn(AndroidSchedulers.mainThread())
            //TODO: what if this fails? Retry button vs Rx.retry
            .subscribeBy(onSuccess = {
                layout_safe_recovery_phrase_mnemonic.text = it
            }, onError = Timber::e)

        disposables += layout_safe_recovery_phrase_reveal.clicks()
            .subscribeBy(onNext = {
                layout_safe_recovery_phrase_container.visibility = View.VISIBLE
                layout_safe_recovery_phrase_reveal.visibility = View.GONE
            }, onError = Timber::e)

        disposables += layout_safe_recovery_phrase_save.clicks()
            .subscribeBy(onNext = { showConfirmationDialog(layout_safe_recovery_phrase_mnemonic.text.toString()) })

        disposables += confirmDialogClick
            .flatMapSingle { viewModel.getRecoveryAddress(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = { recoveryAddress ->
                startActivity(CreateSafeActivity.createIntent(this, chromeExtension, recoveryAddress))
            }, onError = Timber::e)
    }

    private fun showConfirmationDialog(mnemonic: String) {
        AlertDialog.Builder(this)
            .setPositiveButton(getString(R.string.yes), { _, _ -> confirmDialogClick.onNext(mnemonic) })
            .setNegativeButton(getString(R.string.no), { _, _ -> })
            .setTitle(getString(R.string.dialog_title_save_mnemonic))
            .setMessage(Html.fromHtml(resources.getString(R.string.generate_mnemonic_activity_dialog, mnemonic)))
            .show()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        private const val CHROME_EXTENSION_ADDRESS_EXTRA = "extra.string.chrome_extension_address"

        fun createIntent(context: Context, chromeExtension: Solidity.Address) = Intent(context, SafeRecoveryPhraseActivity::class.java).apply {
            putExtra(CHROME_EXTENSION_ADDRESS_EXTRA, chromeExtension.asEthereumAddressString())
        }
    }
}
