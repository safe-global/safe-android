package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import pm.gnosis.heimdall.ui.recoveryphrase.RecoveryPhraseIntroActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow

class SetupNewRecoveryPhraseIntroActivity : RecoveryPhraseIntroActivity() {
    private lateinit var browserExtensionAddress: Solidity.Address
    private lateinit var safeAddress: Solidity.Address

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nullOnThrow { intent.getStringExtra(EXTRA_BROWSER_EXTENSION_ADDRESS)?.let { browserExtensionAddress = it.asEthereumAddress()!! } }
            ?: run { finish(); return }
        nullOnThrow { intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.let { safeAddress = it.asEthereumAddress()!! } }
            ?: run { finish(); return }
    }

    override fun onNextClicked() = startActivity(SetupNewRecoveryPhraseActivity.createIntent(this, safeAddress, browserExtensionAddress))

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_BROWSER_EXTENSION_ADDRESS = "extra.string.browser_extension_address"

        fun createIntent(context: Context, browserExtensionAddress: Solidity.Address, safeAddress: Solidity.Address) =
            Intent(context, SetupNewRecoveryPhraseIntroActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
                putExtra(EXTRA_BROWSER_EXTENSION_ADDRESS, browserExtensionAddress.asEthereumAddressString())
            }
    }
}
