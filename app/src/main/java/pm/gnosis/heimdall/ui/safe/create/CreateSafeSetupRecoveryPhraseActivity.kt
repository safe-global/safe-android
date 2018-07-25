package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseContract
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class CreateSafeSetupRecoveryPhraseActivity : SetupRecoveryPhraseActivity<SetupRecoveryPhraseContract>() {
    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onConfirmedRecoveryPhrase(recoveryPhrase: String) {
        intent.getStringExtra(EXTRA_BROWSER_EXTENSION_ADDRESS)?.asEthereumAddress()?.let { browserExtensionAddress ->
            startActivity(
                CreateSafeConfirmRecoveryPhraseActivity.createIntent(
                    this,
                    recoveryPhrase,
                    browserExtensionAddress
                )
            )
        } ?: run {
            finish(); return
        }
    }

    companion object {
        const val EXTRA_BROWSER_EXTENSION_ADDRESS = "extra.string.browser_extension_address"

        fun createIntent(context: Context, browserExtensionAddress: Solidity.Address) =
            Intent(context, CreateSafeSetupRecoveryPhraseActivity::class.java).apply {
                putExtra(EXTRA_BROWSER_EXTENSION_ADDRESS, browserExtensionAddress.asEthereumAddressString())
            }
    }
}
