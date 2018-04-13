package pm.gnosis.heimdall.ui.settings.security.revealmnemonic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_reveal_mnemonic.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.heimdall.utils.errorToast
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import timber.log.Timber
import javax.inject.Inject

class RevealMnemonicActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: RevealMnemonicContract

    override fun screenId() = ScreenId.REVEAL_MNEMONIC

    private var confirmedUnlock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_reveal_mnemonic)
        setupToolbar(layout_reveal_mnemonic_toolbar)

        layout_reveal_mnemonic_mnemonic.disableAccessibility()
        layout_reveal_mnemonic_mnemonic.setOnLongClickListener {
            copyToClipboard("mnemonic", layout_reveal_mnemonic_mnemonic.text.toString(), {
                snackbar(layout_reveal_mnemonic_coordinator, getString(R.string.mnemonic_copied))
            })
            true
        }
    }

    override fun onStart() {
        super.onStart()
        setUi(showMnemonic = false)

        disposables += layout_reveal_mnemonic_button.clicks()
            .subscribeBy(onNext = {
                startActivityForResult(UnlockActivity.createConfirmIntent(this), UNLOCK_REQUEST)
            }, onError = Timber::e)

        if (confirmedUnlock) {
            setUi(showMnemonic = true)
            confirmedUnlock = false
            disposables += viewModel.loadMnemonic()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = ::onMnemonic, onError = ::onMnemonicError)
        }
    }

    private fun setUi(showMnemonic: Boolean) {
        layout_reveal_mnemonic_button.visibility = if (showMnemonic) View.GONE else View.VISIBLE
        layout_reveal_mnemonic_mnemonic.visibility = if (showMnemonic) View.VISIBLE else View.INVISIBLE
        if (!showMnemonic) {
            layout_reveal_mnemonic_mnemonic.text = ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RevealMnemonicActivity.UNLOCK_REQUEST && resultCode == Activity.RESULT_OK) {
            confirmedUnlock = true
        }
    }

    private fun onMnemonic(mnemonic: String) {
        layout_reveal_mnemonic_mnemonic.text = mnemonic
    }

    private fun onMnemonicError(throwable: Throwable) {
        // If there's an error loading the mnemonic we end the Activity
        // Depending on the design choices this might change (ie.: retry button)
        Timber.e(throwable)
        errorToast(throwable)
        finish()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build()
            .inject(this)
    }

    companion object {
        const val UNLOCK_REQUEST = 12

        fun createIntent(context: Context) = Intent(context, RevealMnemonicActivity::class.java)
    }
}
