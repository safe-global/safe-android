package pm.gnosis.heimdall.ui.safe.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.put
import pm.gnosis.model.Solidity

class CreateSafePairingActivity : PairingActivity() {
    override fun titleRes(): Int = R.string.connect

    override fun shouldShowLaterOption() = false

    override fun onSuccess(signingOwner: AccountsRepository.SafeOwner, extension: Solidity.Address) {
        val authenticatorInfo = AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, extension, signingOwner)
        setResult(Activity.RESULT_OK, authenticatorInfo.put(Intent()))
        finish()
    }

    override fun signingSafe(): Solidity.Address? = null

    companion object {
        fun createIntent(context: Context) = Intent(context, CreateSafePairingActivity::class.java)
    }
}
