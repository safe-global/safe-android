package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.model.Solidity

class CreationPairingActivity: PairingActivity() {
    override fun titleRes(): Int = R.string.new_safe

    override fun onSuccess(extension: Solidity.Address) {
        startActivity(SafeRecoveryPhraseActivity.createIntent(this, extension))
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CreationPairingActivity::class.java)
    }
}
