package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_recovery_phrase_intro.*
import pm.gnosis.heimdall.ui.recoveryphrase.RecoveryPhraseIntroActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.heimdall.utils.put
import pm.gnosis.heimdall.views.StepState

class CreateSafeRecoveryPhraseIntroActivity : RecoveryPhraseIntroActivity() {
    private var authenticatorInfo: AuthenticatorSetupInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticatorInfo = intent.getAuthenticatorInfo()

        step2.state = if (authenticatorInfo == null) StepState.SKIPPED else StepState.COMPLETED
    }

    override fun onNextClicked() = startActivity(CreateSafeSetupRecoveryPhraseActivity.createIntent(this, authenticatorInfo))

    companion object {
        fun createIntent(context: Context, authenticatorInfo: AuthenticatorSetupInfo?) =
            Intent(context, CreateSafeRecoveryPhraseIntroActivity::class.java).apply {
                authenticatorInfo.put(this)
            }
    }
}
