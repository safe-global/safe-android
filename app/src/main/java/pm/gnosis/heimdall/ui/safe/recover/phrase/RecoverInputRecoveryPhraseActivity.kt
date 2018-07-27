package pm.gnosis.heimdall.ui.safe.recover.phrase

import android.content.Context
import android.content.Intent
import kotlinx.android.synthetic.main.layout_input_recovery_phrase.*
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible

class RecoverInputRecoveryPhraseActivity : InputRecoveryPhraseActivity<RecoverInputRecoveryPhraseContract>() {

    //TODO
    override fun noRecoveryNecessary(safe: Solidity.Address) = finish()

    override fun onSuccess(recoverData: InputRecoveryPhraseContract.ViewUpdate.RecoverData) {
        layout_input_recovery_phrase_next.isEnabled = true
        layout_input_recovery_phrase_input_group.visible(true)
        layout_input_recovery_phrase_progress.visible(false)
        startActivity(SafeMainActivity.createIntent(this, recoverData.executionInfo.transaction.wrapped.address))
    }

    override fun screenId() = ScreenId.INPUT_RECOVERY_PHRASE

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        fun createIntent(context: Context, safeAddress: Solidity.Address, extensionAddress: Solidity.Address) =
            InputRecoveryPhraseActivity.addExtras(Intent(context, RecoverInputRecoveryPhraseActivity::class.java), safeAddress, extensionAddress)
    }
}
