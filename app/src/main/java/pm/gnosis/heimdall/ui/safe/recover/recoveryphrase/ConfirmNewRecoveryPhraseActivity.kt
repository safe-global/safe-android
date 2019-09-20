package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_confirm_recovery_phrase.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber

class ConfirmNewRecoveryPhraseActivity : ConfirmRecoveryPhraseActivity<ConfirmNewRecoveryPhraseContract>() {
    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeAddress = nullOnThrow { intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress()!! } ?: run { finish(); return }
        val authenticatorAddress = intent.getStringExtra(EXTRA_AUTHENTICATOR_ADDRESS)?.let { it.asEthereumAddress()!! }
        viewModel.setup(safeAddress, authenticatorAddress)
    }

    override fun isRecoveryPhraseConfirmed() {
        disposables += viewModel.loadTransaction()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { onLoadingTransaction(isLoading = true) }
            .doAfterTerminate { onLoadingTransaction(isLoading = false) }
            .subscribeBy(onSuccess = { onTransaction(it.first, it.second) }, onError = ::onTransactionError)
    }

    private fun onTransaction(safeAddress: Solidity.Address, safeTransaction: SafeTransaction) {
        startActivity(
            ReviewTransactionActivity.createIntent(
                this,
                safeAddress,
                txData = TransactionData.ReplaceRecoveryPhrase(safeTransaction)
            )
        )
    }

    private fun onTransactionError(throwable: Throwable) {
        snackbar(layout_confirm_recovery_phrase_coordinator, R.string.confirm_new_recovery_phrase_error)
        Timber.e(throwable)
    }

    private fun onLoadingTransaction(isLoading: Boolean) {
        bottomBarEnabled(!isLoading)
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_AUTHENTICATOR_ADDRESS = "extra.string.authenticator_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address, authenticatorAddress: Solidity.Address?, recoveryPhrase: String) =
            Intent(context, ConfirmNewRecoveryPhraseActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
                putExtra(EXTRA_AUTHENTICATOR_ADDRESS, authenticatorAddress?.asEthereumAddressString())
                putExtra(EXTRA_RECOVERY_PHRASE, recoveryPhrase)
            }
    }
}
