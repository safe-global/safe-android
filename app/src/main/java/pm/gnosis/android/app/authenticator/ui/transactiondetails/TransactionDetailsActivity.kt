package pm.gnosis.android.app.authenticator.ui.transactiondetails

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_transaction_details.*
import kotlinx.android.synthetic.main.activity_transaction_details_add_owner.view.*
import kotlinx.android.synthetic.main.activity_transaction_details_change_confirmations.view.*
import kotlinx.android.synthetic.main.activity_transaction_details_change_daily_limit.view.*
import kotlinx.android.synthetic.main.activity_transaction_details_remove_owner.view.*
import kotlinx.android.synthetic.main.activity_transaction_details_replace_owner.view.*
import kotlinx.android.synthetic.main.activity_transaction_details_transfer.view.*
import kotlinx.android.synthetic.main.dialog_multisig_add_input.view.*
import pm.gnosis.android.app.authenticator.GnosisAuthenticatorApplication
import pm.gnosis.android.app.authenticator.R
import pm.gnosis.android.app.authenticator.data.contracts.GnosisMultisigWrapper
import pm.gnosis.android.app.authenticator.data.db.MultisigWallet
import pm.gnosis.android.app.authenticator.data.model.TransactionDetails
import pm.gnosis.android.app.authenticator.data.model.Wei
import pm.gnosis.android.app.authenticator.di.component.DaggerViewComponent
import pm.gnosis.android.app.authenticator.di.module.ViewModule
import pm.gnosis.android.app.authenticator.util.*
import timber.log.Timber
import javax.inject.Inject

class TransactionDetailsActivity : AppCompatActivity() {
    companion object {
        const val TRANSACTION_EXTRA = "extra.transaction"
    }

    @Inject lateinit var presenter: TransactionDetailsPresenter
    private lateinit var transaction: TransactionDetails
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_transaction_details)

        val t: TransactionDetails? = intent.extras?.getParcelable(TRANSACTION_EXTRA)
        if (t?.data == null) {
            finish()
            return
        } else {
            transaction = t
        }

        when {
            t.data.isSolidityMethod(GnosisMultisigWrapper.CONFIRM_TRANSACTION_METHOD_ID) -> {
                activity_transaction_details_button.text = "Confirm Transaction"
                activity_transaction_details_transaction_id.text = GnosisMultisigWrapper.decodeConfirm(t.data)?.asDecimalString() ?: "-"
            }
            t.data.isSolidityMethod(GnosisMultisigWrapper.REVOKE_TRANSACTION_METHOD_ID) -> {
                activity_transaction_details_button.text = "Revoke Transaction"
                activity_transaction_details_transaction_id.text = GnosisMultisigWrapper.decodeRevoke(t.data)?.asDecimalString() ?: "-"
            }
            else -> activity_transaction_details_button.visibility = View.GONE
        }
        activity_transaction_details_wallet_address.text = t.address.asEthereumAddressString()
    }

    override fun onStart() {
        super.onStart()
        activity_transaction_details_button.setOnClickListener {
            disposables += signTransactionDisposable()
        }

        activity_transaction_details_add_wallet.setOnClickListener {
            showAddWalletDialog()
        }

        activity_transaction_details_transaction_id.text.toString().decimalAsBigIntegerOrNull()?.let {
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

    private fun onTransactionDetails(multiSigTransaction: GnosisMultisigWrapper.Transaction) {
        when (multiSigTransaction) {
            is GnosisMultisigWrapper.TokenTransfer -> Timber.d("It's a Token Transfer")
            is GnosisMultisigWrapper.Transfer -> onTransfer(multiSigTransaction)
            is GnosisMultisigWrapper.ChangeDailyLimit -> onChangeDailyLimit(multiSigTransaction)
            is GnosisMultisigWrapper.ReplaceOwner -> onReplaceOwner(multiSigTransaction)
            is GnosisMultisigWrapper.AddOwner -> onAddOwner(multiSigTransaction)
            is GnosisMultisigWrapper.RemoveOwner -> onRemoveOwner(multiSigTransaction)
            is GnosisMultisigWrapper.ChangeConfirmations -> onChangeConfirmations(multiSigTransaction)
        }
    }

    private fun onTransfer(transaction: GnosisMultisigWrapper.Transfer) {
        val view = layoutInflater.inflate(R.layout.activity_transaction_details_transfer, activity_transaction_details_coordinator, false)
        activity_transaction_details_action_container.addView(view)
        view.activity_transaction_details_transfer_amount.text = transaction.value.toEther().stripTrailingZeros().toPlainString()
        view.activity_transaction_details_transfer_recipient.text = transaction.address.asEthereumAddressString()
    }

    private fun onAddOwner(transaction: GnosisMultisigWrapper.AddOwner) {
        val view = layoutInflater.inflate(R.layout.activity_transaction_details_add_owner, activity_transaction_details_coordinator, false)
        activity_transaction_details_action_container.addView(view)
        view.activity_transaction_details_add_owner_address.text = transaction.owner.asEthereumAddressString()
    }

    private fun onRemoveOwner(transaction: GnosisMultisigWrapper.RemoveOwner) {
        val view = layoutInflater.inflate(R.layout.activity_transaction_details_remove_owner, activity_transaction_details_coordinator, false)
        activity_transaction_details_action_container.addView(view)
        view.activity_transaction_details_remove_owner_address.text = transaction.owner.asEthereumAddressString()
    }

    private fun onChangeConfirmations(transaction: GnosisMultisigWrapper.ChangeConfirmations) {
        val view = layoutInflater.inflate(R.layout.activity_transaction_details_change_confirmations, activity_transaction_details_coordinator, false)
        activity_transaction_details_action_container.addView(view)
        view.activity_transaction_details_change_confirmations_number.text = transaction.newConfirmations.asDecimalString()
    }

    private fun onChangeDailyLimit(transaction: GnosisMultisigWrapper.ChangeDailyLimit) {
        val view = layoutInflater.inflate(R.layout.activity_transaction_details_change_daily_limit, activity_transaction_details_coordinator, false)
        activity_transaction_details_action_container.addView(view)
        view.activity_transaction_details_change_daily_limit_value.text = Wei(transaction.newDailyLimit).toEther().stripTrailingZeros().toPlainString()
    }

    private fun onReplaceOwner(transaction: GnosisMultisigWrapper.ReplaceOwner) {
        val view = layoutInflater.inflate(R.layout.activity_transaction_details_replace_owner, activity_transaction_details_coordinator, false)
        activity_transaction_details_action_container.addView(view)
        view.activity_transaction_details_replace_owner_old_owner.text = transaction.owner.asEthereumAddressString()
        view.activity_transaction_details_replace_owner_new_owner.text = transaction.newOwner.asEthereumAddressString()
    }

    private fun onTransactionDetailsLoading(isLoading: Boolean) {
        activity_transaction_details_button.isEnabled = !isLoading
        activity_transaction_details_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
        snackbar(activity_transaction_details_coordinator, "Added MultisigWallet")
    }

    private fun onMultisigWalletAddError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(activity_transaction_details_coordinator, "Could not add MultisigWallet")
    }

    private fun onMultisigWallet(multisigWallet: MultisigWallet) {
        activity_transaction_details_wallet_name.text = if (multisigWallet.name.isNullOrEmpty()) "Multisig Address" else multisigWallet.name
        activity_transaction_details_add_wallet.visibility = View.GONE
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
            if (it.isSolidityMethod(GnosisMultisigWrapper.CONFIRM_TRANSACTION_METHOD_ID)) {
                toast("Transaction confirmed")
            } else if (it.isSolidityMethod(GnosisMultisigWrapper.REVOKE_TRANSACTION_METHOD_ID)) {
                toast("Transaction revoked")
            }
        }
        finish()
    }

    private fun onSendTransactionError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(activity_transaction_details_coordinator, "Something went wrong. Transaction not completed.")
    }

    private fun onSendTransactionLoading(isLoading: Boolean) {
        activity_transaction_details_button.isEnabled = !isLoading
        activity_transaction_details_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisAuthenticatorApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }
}
