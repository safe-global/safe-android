package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseContract
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.heimdall.utils.put

class CreateSafeSetupRecoveryPhraseActivity : SetupRecoveryPhraseActivity<SetupRecoveryPhraseContract>() {
    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onConfirmedRecoveryPhrase(recoveryPhrase: String) {
        startActivity(
            CreateSafeConfirmRecoveryPhraseActivity.createIntent(
                context = this,
                recoveryPhrase = recoveryPhrase,
                authenticatorInfo = intent.getAuthenticatorInfo()
            )
        )
    }

    companion object {

        fun createIntent(context: Context, authenticatorInfo: AuthenticatorSetupInfo?) =
            Intent(context, CreateSafeSetupRecoveryPhraseActivity::class.java).apply {
                authenticatorInfo.put(this)
            }
    }
}
