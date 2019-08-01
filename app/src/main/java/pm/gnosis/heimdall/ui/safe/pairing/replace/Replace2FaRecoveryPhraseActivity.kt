package pm.gnosis.heimdall.ui.safe.pairing.replace

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.pairing.PairingSubmitActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.PairingAction
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast

class Replace2FaRecoveryPhraseActivity : InputRecoveryPhraseActivity<Replace2FaRecoveryPhraseContract>() {
    override fun onSuccess(recoverData: InputRecoveryPhraseContract.ViewUpdate.RecoverData) {
        if (recoverData.signatures.size != 2) {
            finish()
            return
        }

        startActivity(
            PairingSubmitActivity.createIntent(
                this,
                safeTransaction = recoverData.executionInfo.transaction,
                signature1 = recoverData.signatures[0],
                signature2 = recoverData.signatures[1],
                txGas = recoverData.executionInfo.txGas,
                dataGas = recoverData.executionInfo.dataGas,
                operationalGas = recoverData.executionInfo.operationalGas,
                gasPrice = recoverData.executionInfo.gasPrice,
                gasToken = recoverData.executionInfo.gasToken,
                authenticatorSetupInfo = intent.getAuthenticatorInfo()!!,
                txHash = recoverData.executionInfo.transactionHash,
                action = PairingAction.REPLACE
            )
        )
    }


    override fun noRecoveryNecessary(safe: Solidity.Address) = toast(R.string.no_recovery_necessary)

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.REPLACE_BROWSER_EXTENSION_RECOVERY_PHRASE

    companion object {
        fun createIntent(context: Context, safeAddress: Solidity.Address, authenticatorInfo: AuthenticatorSetupInfo) =
            addExtras(
                Intent(context, Replace2FaRecoveryPhraseActivity::class.java),
                safeAddress,
                authenticatorInfo
            )
    }
}
