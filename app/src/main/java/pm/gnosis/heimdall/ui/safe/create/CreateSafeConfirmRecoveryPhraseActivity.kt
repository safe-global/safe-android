package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber

class CreateSafeConfirmRecoveryPhraseActivity : ConfirmRecoveryPhraseActivity<CreateSafeConfirmRecoveryPhraseContract>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(EXTRA_BROWSER_EXTENSION_ADDRESS).asEthereumAddress()?.let { viewModel.setup(it) } ?: run { finish(); return }
    }

    override fun isRecoveryPhraseConfirmed() {
        disposables += viewModel.createSafe()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { bottomBarEnabled(false) }
            .subscribeBy(onSuccess = ::onSafeCreated, onError = ::onSafeCreationError)
    }

    private fun onSafeCreated(address: Solidity.Address) {
        startActivity(SafeMainActivity.createIntent(this, address))
    }

    private fun onSafeCreationError(throwable: Throwable) {
        Timber.e(throwable)
        bottomBarEnabled(true)
        toast(R.string.unknown_error)
    }

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        private const val EXTRA_BROWSER_EXTENSION_ADDRESS = "extra.string.browser_extension_address"

        fun createIntent(context: Context, recoveryPhrase: String, chromeExtensionAddress: Solidity.Address) =
            Intent(context, CreateSafeConfirmRecoveryPhraseActivity::class.java).apply {
                putExtra(EXTRA_RECOVERY_PHRASE, recoveryPhrase)
                putExtra(EXTRA_BROWSER_EXTENSION_ADDRESS, chromeExtensionAddress.asEthereumAddressString())
            }
    }
}
