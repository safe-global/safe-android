package pm.gnosis.heimdall.ui.transactiondetails

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_multisig_add_input.view.*
import kotlinx.android.synthetic.main.layout_transaction_details.*
import kotlinx.android.synthetic.main.layout_transaction_details_add_owner.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_change_confirmations.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_change_daily_limit.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_remove_owner.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_replace_owner.view.*
import kotlinx.android.synthetic.main.layout_transaction_details_token_transfer.*
import kotlinx.android.synthetic.main.layout_transaction_details_transfer.view.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.common.util.snackbar
import pm.gnosis.heimdall.common.util.toast
import pm.gnosis.heimdall.data.contracts.GnosisMultisigWrapper
import pm.gnosis.heimdall.data.db.MultisigWallet
import pm.gnosis.heimdall.data.model.TransactionDetails
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.utils.*
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

class TransactionDetailsActivity : BaseActivity() {
    companion object {
        const val TRANSACTION_EXTRA = "extra.transaction"
    }

    @Inject lateinit var presenter: TransactionDetailsPresenter
    private lateinit var transaction: TransactionDetails

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_transaction_details)

        val t: TransactionDetails? = intent.extras?.getParcelable(TRANSACTION_EXTRA)
        if (t?.data == null) {
            finish()
            return
        } else {
            transaction = t
        }

        when {
            t.data!!.isSolidityMethod(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID) -> {
                layout_transaction_details_button.text = "Confirm Transaction"
                val argument = t.data!!.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID)
                layout_transaction_details_transaction_id.text = MultiSigWalletWithDailyLimit.ConfirmTransaction.decodeArguments(argument).transactionid.value.asDecimalString() ?: "-"
            }
            t.data!!.isSolidityMethod(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID) -> {
                layout_transaction_details_button.text = "Revoke Transaction"
                val argument = t.data!!.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID)
                layout_transaction_details_transaction_id.text = MultiSigWalletWithDailyLimit.RevokeConfirmation.decodeArguments(argument).transactionid.value.asDecimalString() ?: "-"
            }
            else -> layout_transaction_details_button.visibility = View.GONE
        }
        layout_transaction_details_wallet_address.text = t.address.asEthereumAddressString()
    }

    override fun onStart() {
        super.onStart()
        layout_transaction_details_button.setOnClickListener {
            disposables += signTransactionDisposable()
        }

        layout_transaction_details_add_wallet.setOnClickListener {
            showAddWalletDialog()
        }

        layout_transaction_details_transaction_id.text.toString().decimalAsBigIntegerOrNull()?.let {
            presenter.getTransactionDetails(transaction.address.asEthereumAddressString(), it)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { onTransactionDetailsLoading(true) }
                    .doOnTerminate { onTransactionDetailsLoading(false) }
                    .subscribeBy(onNext = this::onTransactionDetails, onError = Timber::e)
        }

        presenter.getMultisigWalletDetails(transaction.address)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onMultisigWallet, onError = this::onMultisigWalletError)
    }

    private fun onTransactionDetails(multiSigTransaction: GnosisMultisigWrapper.WrapperTransaction) {
        when (multiSigTransaction) {
            is GnosisMultisigWrapper.TokenTransfer -> onTokenTransfer(multiSigTransaction)
            is GnosisMultisigWrapper.Transfer -> onTransfer(multiSigTransaction)
            is GnosisMultisigWrapper.ChangeDailyLimit -> onChangeDailyLimit(multiSigTransaction)
            is GnosisMultisigWrapper.ReplaceOwner -> onReplaceOwner(multiSigTransaction)
            is GnosisMultisigWrapper.AddOwner -> onAddOwner(multiSigTransaction)
            is GnosisMultisigWrapper.RemoveOwner -> onRemoveOwner(multiSigTransaction)
            is GnosisMultisigWrapper.ChangeConfirmations -> onChangeConfirmations(multiSigTransaction)
        }
    }

    private fun onTransfer(transaction: GnosisMultisigWrapper.Transfer) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_transfer, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_transfer_amount.text = transaction.value.toEther().stripTrailingZeros().toPlainString()
        view.layout_transaction_details_transfer_recipient.text = transaction.address.asEthereumAddressString()
    }

    private fun onAddOwner(transaction: GnosisMultisigWrapper.AddOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_add_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_add_owner_address.text = transaction.owner.asEthereumAddressString()
    }

    private fun onRemoveOwner(transaction: GnosisMultisigWrapper.RemoveOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_remove_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_remove_owner_address.text = transaction.owner.asEthereumAddressString()
    }

    private fun onChangeConfirmations(transaction: GnosisMultisigWrapper.ChangeConfirmations) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_change_confirmations, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_change_confirmations_number.text = transaction.newConfirmations.asDecimalString()
    }

    private fun onChangeDailyLimit(transaction: GnosisMultisigWrapper.ChangeDailyLimit) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_change_daily_limit, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_change_daily_limit_value.text = Wei(transaction.newDailyLimit).toEther().stripTrailingZeros().toPlainString()
    }

    private fun onReplaceOwner(transaction: GnosisMultisigWrapper.ReplaceOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_replace_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_replace_owner_old_owner.text = transaction.owner.asEthereumAddressString()
        view.layout_transaction_details_replace_owner_new_owner.text = transaction.newOwner.asEthereumAddressString()
    }

    private fun onTokenTransfer(transaction: GnosisMultisigWrapper.TokenTransfer) {
        disposables += presenter.getTokenInfo(transaction.tokenAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = { onTokenTransferInfo(transaction, it) }, onError = Timber::e)
    }

    private fun onTokenTransferInfo(transaction: GnosisMultisigWrapper.TokenTransfer, token: ERC20.Token) {
        if (token.decimals == null) return

        val view = layoutInflater.inflate(R.layout.layout_transaction_details_token_transfer, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        layout_transaction_details_token_transfer_symbol.text = token.symbol ?: "Tokens"
        layout_transaction_details_token_transfer_recipient.text = transaction.recipient.asEthereumAddressString()

        val tokens = BigDecimal(transaction.tokens, token.decimals!!.toInt())
        layout_transaction_details_token_transfer_amount.text = tokens.stripTrailingZeros().toPlainString()
    }

    private fun onTransactionDetailsLoading(isLoading: Boolean) {
        layout_transaction_details_button.isEnabled = !isLoading
        layout_transaction_details_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showAddWalletDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_multisig_add_input, null)

        dialogView.dialog_add_multisig_text_address.setText(transaction.address.asEthereumAddressString())
        dialogView.dialog_add_multisig_text_address.isEnabled = false
        dialogView.dialog_add_multisig_text_address.inputType = InputType.TYPE_NULL


        val dialog = AlertDialog.Builder(this)
                .setTitle("Add a Multisig Wallet")
                .setView(dialogView)
                .setPositiveButton("Add", { _, _ -> })
                .setNegativeButton("Cancel", { _, _ -> })
                .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogView.dialog_add_multisig_text_name.text.toString()
            val address = dialogView.dialog_add_multisig_text_address.text.toString()
            if (address.isValidEthereumAddress()) {
                disposables += addMultisigWalletDisposable(name, address.addAddressPrefix())
                dialog.dismiss()
            } else {
                toast("Invalid ethereum address")
            }
        }
    }

    private fun addMultisigWalletDisposable(name: String, address: String) =
            presenter.addMultisigWallet(name, address)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onMultisigWalletAdded, onError = this::onMultisigWalletAddError)

    private fun onMultisigWalletAdded() {
        snackbar(layout_transaction_details_coordinator, "Added MultisigWallet")
    }

    private fun onMultisigWalletAddError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_transaction_details_coordinator, "Could not add MultisigWallet")
    }

    private fun onMultisigWallet(multisigWallet: MultisigWallet) {
        layout_transaction_details_wallet_name.text = if (multisigWallet.name.isNullOrEmpty()) "Multisig Address" else multisigWallet.name
        layout_transaction_details_add_wallet.visibility = View.GONE
    }

    private fun onMultisigWalletError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun signTransactionDisposable() = presenter.signTransaction(transaction)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { onSendTransactionLoading(true) }
            .doOnTerminate { onSendTransactionLoading(false) }
            .subscribeBy(onNext = this::onSendTransaction, onError = this::onSendTransactionError)

    private fun onSendTransaction(transactionHash: String) {
        transaction.data?.let {
            if (it.isSolidityMethod(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID)) {
                toast("Transaction confirmed")
            } else if (it.isSolidityMethod(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID)) {
                toast("Transaction revoked")
            }
        }
        finish()
    }

    private fun onSendTransactionError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_transaction_details_coordinator, "Something went wrong. Transaction not completed.")
    }

    private fun onSendTransactionLoading(isLoading: Boolean) {
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
