package pm.gnosis.heimdall.ui.safe.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.ui.keycard.KeycardIntroActivity
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.put
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

// TODO: merge with abstract class (title could be parameter)
class CreateSafePairingActivity : PairingActivity() {
    override fun titleRes(): Int = R.string.connect

    override fun shouldShowLaterOption() = false

    override fun onSuccess(signingOwner: AccountsRepository.SafeOwner, extension: Solidity.Address) {
        val authenticatorInfo = AuthenticatorSetupInfo(signingOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, extension))
        setResult(Activity.RESULT_OK, authenticatorInfo.put(Intent()))
        finish()
    }

    override fun signingSafe(): Solidity.Address? = intent.getStringExtra(EXTRA_SAFE)?.asEthereumAddress()

    companion object {
        private const val EXTRA_SAFE = "extra.string.safe"
        fun createIntent(context: Context, safe: Solidity.Address?) = Intent(context, CreateSafePairingActivity::class.java).apply {
            putExtra(EXTRA_SAFE, safe?.asEthereumAddressString())
        }
    }
}
