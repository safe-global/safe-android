package pm.gnosis.heimdall.ui.transactiondetails

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.dialog_multisig_add_input.*
import kotlinx.android.synthetic.main.dialog_multisig_add_input.view.*
import kotlinx.android.synthetic.main.layout_transaction_details.*
import kotlinx.android.synthetic.main.layout_transaction_details_add_owner.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_change_confirmations.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_change_daily_limit.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_remove_owner.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_replace_owner.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_transfer.*
import kotlinx.android.synthetic.main.layout_transaction_details_transfer.view.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.snackbar
import pm.gnosis.heimdall.common.util.subscribeForResult
import pm.gnosis.heimdall.common.util.toast
import pm.gnosis.heimdall.data.contracts.*
import pm.gnosis.heimdall.data.model.TransactionDetails
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.utils.addAddressPrefix
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

class TransactionDetailsActivity : BaseActivity() {
    companion object {
        fun createIntent(context: Context, transactionDetails: TransactionDetails): Intent {
            val intent = Intent(context, TransactionDetailsActivity::class.java)
            intent.putExtra(TransactionDetailsActivity.TRANSACTION_EXTRA, transactionDetails)
            return intent
        }

        const val TRANSACTION_EXTRA = "extra.transaction"
    }

    @Inject lateinit var viewModel: TransactionDetailsContract

    private val addWalletClickSubject = PublishSubject.create<Pair<String, String>>()
    private var transactionSigningInProgress = false
    private var transactionDetailsInProgress = false
    private var tokenInfoInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_transaction_details)
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.setTransaction(intent.extras?.getParcelable(TRANSACTION_EXTRA))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onComplete = this::onValidTransactionDetails, onError = this::onInvalidTransactionDetails)
    }

    private fun onValidTransactionDetails() {
        disposables += signTransactionDisposable()
        disposables += layout_transaction_details_add_wallet.clicks()
                .subscribeBy(onNext = { showAddWalletDialog() }, onError = Timber::e)
        disposables += addMultisigWalletDisposable()
        disposables += transactionDetailsDisposable()
        disposables += multisigWalletDetailsDisposable()

        when (viewModel.getMultisigTransactionType()) {
            is ConfirmMultisigTransaction -> layout_transaction_details_button.text = getString(R.string.confirm_transaction)
            is RevokeMultisigTransaction -> layout_transaction_details_button.text = getString(R.string.revoke_transaction)
        }

        layout_transaction_details_transaction_id.text = getString(R.string.transaction_number,
                viewModel.getMultisigTransactionId().asDecimalString() ?: "-")
        layout_transaction_details_wallet_address.text = viewModel.getTransaction().address.asEthereumAddressString()
    }

    private fun onInvalidTransactionDetails(throwable: Throwable) {
        //TODO change screen to error layout
        Timber.e(throwable)
        finish()
    }

    private fun transactionDetailsDisposable() =
            viewModel.loadTransactionDetails()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { onTransactionDetailsLoading(true) }
                    .doOnTerminate { onTransactionDetailsLoading(false) }
                    .subscribeBy(onNext = this::onTransactionDetails, onError = this::onTransactionDetailsError)

    private fun onTransactionDetailsLoading(isLoading: Boolean) {
        transactionDetailsInProgress = isLoading
        layout_transaction_details_data_container.visibility = if (isLoading) View.GONE else View.VISIBLE
        refreshLoadingLayout()
    }

    private fun onTransactionDetailsError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_transaction_details_coordinator, getString(R.string.transaction_details_error))
    }

    private fun onTransactionDetails(multiSigTransaction: GnosisMultisigTransaction) {
        when (multiSigTransaction) {
            is MultisigTokenTransfer -> onTokenTransfer(multiSigTransaction)
            is MultisigTransfer -> onTransfer(multiSigTransaction)
            is MultisigChangeDailyLimit -> onChangeDailyLimit(multiSigTransaction)
            is MultisigReplaceOwner -> onReplaceOwner(multiSigTransaction)
            is MultisigAddOwner -> onAddOwner(multiSigTransaction)
            is MultisigRemoveOwner -> onRemoveOwner(multiSigTransaction)
            is MultisigChangeConfirmations -> onChangeConfirmations(multiSigTransaction)
        }
    }

    private fun onTransfer(transaction: MultisigTransfer) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_transfer, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_transfer_amount.text = transaction.value.toEther().stripTrailingZeros().toPlainString()
        view.layout_transaction_details_transfer_token_symbol.text = getString(R.string.currency_eth)
        view.layout_transaction_details_transfer_recipient.text = transaction.address.asEthereumAddressString()
    }

    private fun onAddOwner(transaction: MultisigAddOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_add_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_add_owner_address.text = transaction.owner.asEthereumAddressString()
    }

    private fun onRemoveOwner(transaction: MultisigRemoveOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_remove_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_remove_owner_address.text = transaction.owner.asEthereumAddressString()
    }

    private fun onChangeConfirmations(transaction: MultisigChangeConfirmations) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_change_confirmations, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_change_confirmations_number.text = transaction.newConfirmations.asDecimalString()
    }

    private fun onChangeDailyLimit(transaction: MultisigChangeDailyLimit) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_change_daily_limit, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_change_daily_limit_value.text = Wei(transaction.newDailyLimit).toEther().stripTrailingZeros().toPlainString()
    }

    private fun onReplaceOwner(transaction: MultisigReplaceOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_replace_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_replace_owner_old_owner.text = transaction.owner.asEthereumAddressString()
        view.layout_transaction_details_replace_owner_new_owner.text = transaction.newOwner.asEthereumAddressString()
    }

    // Token Transfer
    private fun onTokenTransfer(transaction: MultisigTokenTransfer) {
        disposables += viewModel.loadTokenInfo(transaction.tokenAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { onTokenInfoLoading(true) }
                .doOnTerminate { onTokenInfoLoading(false) }
                .subscribeBy(onNext = { onTokenTransferInfo(transaction, it) }, onError = this::onTokenInfoError)
    }

    private fun onTokenInfoLoading(isLoading: Boolean) {
        tokenInfoInProgress = isLoading
        refreshLoadingLayout()
    }

    private fun onTokenInfoError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_transaction_details_coordinator, getString(R.string.transaction_details_error))
    }

    private fun onTokenTransferInfo(transaction: MultisigTokenTransfer, token: ERC20Token) {
        if (token.decimals == null) return

        val view = layoutInflater.inflate(R.layout.layout_transaction_details_transfer, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)

        val tokens = BigDecimal(transaction.tokens, token.decimals.toInt())
        layout_transaction_details_transfer_amount.text = tokens.stripTrailingZeros().toPlainString()
        view.layout_transaction_details_transfer_token_symbol.text = token.symbol ?: getString(R.string.tokens)
        layout_transaction_details_transfer_recipient.text = transaction.recipient.asEthereumAddressString()
    }

    // Add Multisig Wallet
    private fun showAddWalletDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_multisig_add_input, null)

        dialogView.dialog_add_multisig_text_address.setText(viewModel.getTransaction().address.asEthereumAddressString())
        dialogView.dialog_add_multisig_text_address.isEnabled = false
        dialogView.dialog_add_multisig_text_address.inputType = InputType.TYPE_NULL

        val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.add_wallet)
                .setView(dialogView)
                .setPositiveButton(R.string.add, { _, _ -> })
                .setNegativeButton(R.string.cancel, { _, _ -> })
                .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogView.dialog_add_multisig_text_name.text.toString()
            val address = dialogView.dialog_add_multisig_text_address.text.toString()
            if (address.isValidEthereumAddress()) {
                addWalletClickSubject.onNext(address.addAddressPrefix() to name)
                dialog.dismiss()
            } else {
                dialog_add_multisig_text_input_layout.error = getString(R.string.invalid_ethereum_address)
            }
        }
    }

    private fun addMultisigWalletDisposable() =
            addWalletClickSubject
                    .flatMapSingle { viewModel.addMultisigWallet(it.first, it.second) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeForResult(onNext = { onMultisigWalletAdded() }, onError = this::onMultisigWalletAddError)

    private fun onMultisigWalletAdded() {
        snackbar(layout_transaction_details_coordinator, getString(R.string.added_multisig))
    }

    private fun onMultisigWalletAddError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_transaction_details_coordinator, getString(R.string.add_multisig_wallet_error))
    }

    // Multisig Wallet Details
    private fun multisigWalletDetailsDisposable() =
            viewModel.observeMultisigWalletDetails()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onNext = this::onMultisigWallet, onError = this::onMultisigWalletError)

    private fun onMultisigWallet(multisigWallet: MultisigWallet) {
        layout_transaction_details_wallet_name.text = if (multisigWallet.name.isNullOrEmpty()) getString(R.string.multisig_address) else multisigWallet.name
        layout_transaction_details_add_wallet.visibility = View.GONE
    }

    private fun onMultisigWalletError(throwable: Throwable) {
        Timber.e(throwable)
    }

    // Signing Transaction
    private fun signTransactionDisposable() =
            layout_transaction_details_button.clicks()
                    .flatMap {
                        viewModel.signTransaction()
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnSubscribe { onTransactionSignLoading(true) }
                                .doOnTerminate { onTransactionSignLoading(false) }
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeForResult(onNext = this::onTransactionSigned, onError = this::onTransactionSignError)

    private fun onTransactionSigned(transactionHash: String) {
        when (viewModel.getMultisigTransactionType()) {
            is ConfirmMultisigTransaction -> toast(getString(R.string.transaction_confirmed))
            is RevokeMultisigTransaction -> toast(getString(R.string.transaction_revoked))
        }
        finish()
    }

    private fun onTransactionSignError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_transaction_details_coordinator, getString(R.string.transaction_error))
    }

    private fun onTransactionSignLoading(isLoading: Boolean) {
        transactionSigningInProgress = isLoading
        refreshLoadingLayout()
    }

    private fun refreshLoadingLayout() {
        val isLoading = transactionSigningInProgress || transactionDetailsInProgress || tokenInfoInProgress
        layout_transaction_details_button.isEnabled = !isLoading
        layout_transaction_details_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }
}
