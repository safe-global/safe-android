package pm.gnosis.heimdall.ui.safe.pairing.remove

import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.pairing.PairingSubmitActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.PairingAction
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.model.Solidity
import javax.inject.Inject

class Remove2FaRecoveryPhraseActivity : InputRecoveryPhraseActivity<Remove2FaRecoveryPhraseContract>() {

    override fun onSuccess(recoverData: InputRecoveryPhraseContract.ViewUpdate.RecoverData) {
        if (recoverData.signatures.size != 2) {
            finish()
            return
        }

        startActivity(
            PairingSubmitActivity.createIntent(
                this,
                PairingAction.REMOVE,
                safeTransaction = recoverData.executionInfo.transaction,
                signature1 = recoverData.signatures[0],
                signature2 = recoverData.signatures[1],
                txGas = recoverData.executionInfo.txGas,
                dataGas = recoverData.executionInfo.dataGas,
                operationalGas = recoverData.executionInfo.operationalGas,
                gasPrice = recoverData.executionInfo.gasPrice,
                gasToken = recoverData.executionInfo.gasToken,
                authenticatorSetupInfo = intent.getAuthenticatorInfo()!!,
                txHash = recoverData.executionInfo.transactionHash
            )
        )
    }

    override fun noRecoveryNecessary(safe: Solidity.Address) {}

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.REMOVE_2FA_RECOVERY_PHRASE

    companion object {
        fun createIntent(context: Context, safeAddress: Solidity.Address, authenticatorInfo: AuthenticatorSetupInfo) =
            addExtras(
                Intent(context, Remove2FaRecoveryPhraseActivity::class.java),
                safeAddress,
                authenticatorInfo
            )
    }
}

abstract class Remove2FaRecoveryPhraseContract : InputRecoveryPhraseContract()

class Remove2FaRecoveryPhraseViewModel @Inject constructor(
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper
) : Remove2FaRecoveryPhraseContract() {
    override fun process(input: Input, safeAddress: Solidity.Address, authenticatorInfo: AuthenticatorSetupInfo?): Observable<ViewUpdate> =
        authenticatorInfo?.let {
            recoverSafeOwnersHelper.process(input, safeAddress, null, authenticatorInfo.safeOwner)
        } ?: Observable.just<ViewUpdate>(ViewUpdate.RecoverDataError(IllegalStateException("Authenticator is required!")))
}

