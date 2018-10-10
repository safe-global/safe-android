package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseContract
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow

class SetupNewRecoveryPhraseActivity : SetupRecoveryPhraseActivity<SetupRecoveryPhraseContract>() {
    private lateinit var safeAddress: Solidity.Address
    private lateinit var browserAddress: Solidity.Address

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safeAddress = nullOnThrow { intent.getStringExtra(EXTRA_SAFE_ADDRESS).asEthereumAddress()!! } ?: run { finish(); return }
        browserAddress = nullOnThrow { intent.getStringExtra(EXTRA_BROWSER_ADDRESS).asEthereumAddress()!! } ?: run { finish(); return }
    }

    override fun onConfirmedRecoveryPhrase(recoveryPhrase: String) {
        startActivity(ConfirmNewRecoveryPhraseActivity.createIntent(this, safeAddress, browserAddress, recoveryPhrase))
    }

    override fun inject(component: ViewComponent) = viewComponent().inject(this)

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_BROWSER_ADDRESS = "extra.string.browser_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address, browserAddress: Solidity.Address) =
            Intent(context, SetupNewRecoveryPhraseActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
                putExtra(EXTRA_BROWSER_ADDRESS, browserAddress.asEthereumAddressString())
            }
    }
}
