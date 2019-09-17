package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import pm.gnosis.heimdall.ui.recoveryphrase.RecoveryPhraseIntroActivity
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.heimdall.utils.put

class CreateSafeRecoveryPhraseIntroActivity : RecoveryPhraseIntroActivity() {
    private var authenticatorInfo: AuthenticatorInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticatorInfo = intent.getAuthenticatorInfo()
    }

    override fun onNextClicked() = startActivity(CreateSafeSetupRecoveryPhraseActivity.createIntent(this, authenticatorInfo))

    companion object {
        fun createIntent(context: Context, authenticatorInfo: AuthenticatorInfo?) =
            Intent(context, CreateSafeRecoveryPhraseIntroActivity::class.java).apply {
                authenticatorInfo.put(this)
            }
    }
}
