package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseContract
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class CreateSafeSetupRecoveryPhraseActivity : SetupRecoveryPhraseActivity<SetupRecoveryPhraseContract>() {
    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onConfirmedRecoveryPhrase(recoveryPhrase: String) {
        startActivity(
            CreateSafeConfirmRecoveryPhraseActivity.createIntent(
                context = this,
                recoveryPhrase = recoveryPhrase,
                browserExtensionAddress = intent.getStringExtra(EXTRA_BROWSER_EXTENSION_ADDRESS)?.let { it.asEthereumAddress()!! },
                safeOwner = intent.getParcelableExtra(EXTRA_SAFE_OWNER)
            )
        )
    }

    companion object {
        private const val EXTRA_BROWSER_EXTENSION_ADDRESS = "extra.string.browser_extension_address"
        private const val EXTRA_SAFE_OWNER = "extra.parcelable.safe_owner"

        fun createIntent(context: Context, browserExtensionAddress: Solidity.Address?, safeOwner: AccountsRepository.SafeOwner?) =
            Intent(context, CreateSafeSetupRecoveryPhraseActivity::class.java).apply {
                putExtra(EXTRA_BROWSER_EXTENSION_ADDRESS, browserExtensionAddress?.asEthereumAddressString())
                putExtra(EXTRA_SAFE_OWNER, safeOwner)
            }
    }
}
