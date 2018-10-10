package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.model.Solidity

class CreateSafePairingActivity : PairingActivity() {
    override fun titleRes(): Int = R.string.connect

    override fun onSuccess(extension: Solidity.Address) {
        startActivity(CreateSafeRecoveryPhraseIntroActivity.createIntent(this, extension))
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CreateSafePairingActivity::class.java)
    }
}
