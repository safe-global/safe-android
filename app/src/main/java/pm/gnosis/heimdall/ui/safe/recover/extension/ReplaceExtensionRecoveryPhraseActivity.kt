package pm.gnosis.heimdall.ui.safe.recover.extension

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress

class ReplaceExtensionRecoveryPhraseActivity : InputRecoveryPhraseActivity<ReplaceExtensionRecoveryPhraseContract>() {
    override fun onSuccess(recoverData: InputRecoveryPhraseContract.ViewUpdate.RecoverData) {
        if (recoverData.signatures.size != 2) {
            finish()
            return
        }

        startActivity(
            ReplaceExtensionSubmitActivity.createIntent(
                this,
                safeTransaction = recoverData.executionInfo.transaction,
                signature1 = recoverData.signatures[0],
                signature2 = recoverData.signatures[1],
                txGas = recoverData.executionInfo.txGas,
                dataGas = recoverData.executionInfo.dataGas,
                gasPrice = recoverData.executionInfo.gasPrice,
                chromeExtensionAddress = intent.getStringExtra(EXTRA_EXTENSION_ADDRESS).asEthereumAddress()!!,
                txHash = recoverData.executionInfo.transactionHash
            )
        )
    }


    override fun noRecoveryNecessary(safe: Solidity.Address) = toast(R.string.no_recovery_necessary)

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.REPLACE_BROWSER_EXTENSION_RECOVERY_PHRASE

    companion object {
        fun createIntent(context: Context, safeAddress: Solidity.Address, extensionAddress: Solidity.Address) =
            InputRecoveryPhraseActivity.addExtras(
                Intent(context, ReplaceExtensionRecoveryPhraseActivity::class.java),
                safeAddress,
                extensionAddress
            )
    }
}
