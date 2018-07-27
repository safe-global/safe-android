package pm.gnosis.heimdall.ui.safe.recover.phrase

import android.content.Context
import android.content.Intent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_input_recovery_phrase.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.recover.ReplaceBrowserExtensionActivity
import pm.gnosis.heimdall.utils.errorToast
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexStringToByteArray
import timber.log.Timber

class ReplaceBrowserExtensionRecoveryPhraseActivity : InputRecoveryPhraseActivity<ReplaceBrowserExtensionRecoveryPhraseContract>() {
    override fun onSuccess(recoverData: InputRecoveryPhraseContract.ViewUpdate.RecoverData) {
        if (recoverData.signatures.size != 2) {
            finish(); return
        }
        disposables +=
                viewModel.recoverAddresses(recoverData.executionInfo.transactionHash.hexStringToByteArray(), recoverData.signatures)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { layout_input_recovery_phrase_progress.visible(true) }
                    .doAfterTerminate { layout_input_recovery_phrase_progress.visible(false) }
                    .subscribeBy(onSuccess = {
                        if (it.size == 2) {
                            startActivity(
                                ReplaceBrowserExtensionActivity.createIntent(
                                    this,
                                    safeTransaction = recoverData.executionInfo.transaction,
                                    signature1 = it[0],
                                    signature2 = it[1],
                                    txGas = recoverData.executionInfo.txGas,
                                    dataGas = recoverData.executionInfo.dataGas,
                                    gasPrice = recoverData.executionInfo.gasPrice,
                                    chromeExtensionAddress = intent.getStringExtra(EXTRA_EXTENSION_ADDRESS).asEthereumAddress()!!
                                )
                            )
                        }
                    }, onError = {
                        Timber.e(it)
                        errorToast(it)
                        finish()
                    })
    }

    override fun noRecoveryNecessary(safe: Solidity.Address) = toast(R.string.no_recovery_necessary)

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.REPLACE_BROWSER_EXTENSION_RECOVERY_PHRASE

    companion object {
        fun createIntent(context: Context, safeAddress: Solidity.Address, extensionAddress: Solidity.Address) =
            InputRecoveryPhraseActivity.addExtras(
                Intent(context, ReplaceBrowserExtensionRecoveryPhraseActivity::class.java),
                safeAddress,
                extensionAddress
            )
    }
}
