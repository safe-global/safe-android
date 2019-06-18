package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.ui.recoveryphrase.RecoveryPhraseIntroActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class CreateSafeRecoveryPhraseIntroActivity : RecoveryPhraseIntroActivity() {
    private var browserExtensionAddress: Solidity.Address? = null
    private var safeOwner: AccountsRepository.SafeOwner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(EXTRA_BROWSER_EXTENSION_ADDRESS)?.let { browserExtensionAddress = it.asEthereumAddress()!! }
        safeOwner = intent.getParcelableExtra(EXTRA_SAFE_OWNER)
    }

    override fun onNextClicked() = startActivity(CreateSafeSetupRecoveryPhraseActivity.createIntent(this, browserExtensionAddress, safeOwner))

    companion object {
        private const val EXTRA_BROWSER_EXTENSION_ADDRESS = "extra.string.browser_extension_address"
        private const val EXTRA_SAFE_OWNER = "extra.parcelable.safe_owner"

        fun createIntent(context: Context, browserExtensionAddress: Solidity.Address?, safeOwner: AccountsRepository.SafeOwner?) =
            Intent(context, CreateSafeRecoveryPhraseIntroActivity::class.java).apply {
                putExtra(EXTRA_BROWSER_EXTENSION_ADDRESS, browserExtensionAddress?.asEthereumAddressString())
                putExtra(EXTRA_SAFE_OWNER, safeOwner)
            }
    }
}
