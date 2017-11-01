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
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

class TransactionDetailsActivity : BaseActivity() {
    companion object {

        private const val TRANSACTION_EXTRA = "extra.parcelable.transaction"
        private const val DESCRIPTION_EXTRA = "extra.string.description"

        fun createIntent(context: Context, transaction: Transaction, descriptionHash: String?): Intent {
            val intent = Intent(context, TransactionDetailsActivity::class.java)
            intent.putExtra(TRANSACTION_EXTRA, transaction.parcelable())
            intent.putExtra(DESCRIPTION_EXTRA, descriptionHash)
            return intent
        }
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
        val transaction = intent.extras?.getParcelable<TransactionParcelable>(TRANSACTION_EXTRA)?.transaction
        val descriptionHash = intent.extras?.getString(DESCRIPTION_EXTRA)
        disposables += viewModel.setTransaction(transaction, descriptionHash)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onComplete = this::onValidTransactionDetails, onError = this::onInvalidTransactionDetails)
    }

    private fun onValidTransactionDetails() {
        disposables += signTransactionDisposable()
        disposables += layout_transaction_details_add_wallet.clicks()
                .subscribeBy(onNext = { showAddSafeDialog() }, onError = Timber::e)
        disposables += addSafeDisposable()
        disposables += transactionDetailsDisposable()
        disposables += safeDetailsDisposable()

        when (viewModel.getTransactionType()) {
            is ConfirmMultisigTransaction -> layout_transaction_details_button.text = getString(R.string.confirm_transaction)
            is RevokeMultisigTransaction -> layout_transaction_details_button.text = getString(R.string.revoke_transaction)
        }

        val shortTransactionHash = viewModel.getTransactionHash().subSequence(0, 6)
        layout_transaction_details_transaction_id.text = getString(R.string.transaction_number, shortTransactionHash)
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

    private fun onTransactionDetails(transactionDetails: TransactionDetails) {
        layout_transaction_details_action_container.removeAllViews()
        when (transactionDetails) {
            is TokenTransfer -> onTokenTransfer(transactionDetails)
            is EtherTransfer -> onTransfer(transactionDetails)
            is SafeChangeDailyLimit -> onChangeDailyLimit(transactionDetails)
            is SafeReplaceOwner -> onReplaceOwner(transactionDetails)
            is SafeAddOwner -> onAddOwner(transactionDetails)
            is SafeRemoveOwner -> onRemoveOwner(transactionDetails)
            is SafeChangeConfirmations -> onChangeConfirmations(transactionDetails)
        }
    }

    private fun onTransfer(transaction: EtherTransfer) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_transfer, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_transfer_amount.text = transaction.value.toEther().stripTrailingZeros().toPlainString()
        view.layout_transaction_details_transfer_token_symbol.text = getString(R.string.currency_eth)
        view.layout_transaction_details_transfer_recipient.text = transaction.address.asEthereumAddressString()
    }

    private fun onAddOwner(transaction: SafeAddOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_add_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_add_owner_address.text = transaction.owner.asEthereumAddressString()
    }

    private fun onRemoveOwner(transaction: SafeRemoveOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_remove_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_remove_owner_address.text = transaction.owner.asEthereumAddressString()
    }

    private fun onChangeConfirmations(transaction: SafeChangeConfirmations) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_change_confirmations, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_change_confirmations_number.text = transaction.newConfirmations.asDecimalString()
    }

    private fun onChangeDailyLimit(transaction: SafeChangeDailyLimit) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_change_daily_limit, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_change_daily_limit_value.text = Wei(transaction.newDailyLimit).toEther().stripTrailingZeros().toPlainString()
    }

    private fun onReplaceOwner(transaction: SafeReplaceOwner) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_replace_owner, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)
        view.layout_transaction_details_replace_owner_old_owner.text = transaction.owner.asEthereumAddressString()
        view.layout_transaction_details_replace_owner_new_owner.text = transaction.newOwner.asEthereumAddressString()
    }

    // Token Transfer
    private fun onTokenTransfer(transaction: TokenTransfer) {
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

    private fun onTokenTransferInfo(transaction: TokenTransfer, token: ERC20Token) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_details_transfer, layout_transaction_details_coordinator, false)
        layout_transaction_details_action_container.addView(view)

        val tokens = BigDecimal(transaction.tokens, token.decimals)
        layout_transaction_details_transfer_amount.text = tokens.stripTrailingZeros().toPlainString()
        view.layout_transaction_details_transfer_token_symbol.text = token.symbol ?: getString(R.string.tokens)
        layout_transaction_details_transfer_recipient.text = transaction.recipient.asEthereumAddressString()
    }

    // Add Multisig Wallet
    private fun showAddSafeDialog() {
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

    private fun addSafeDisposable() =
            addWalletClickSubject
                    .flatMapSingle { viewModel.addSafe(it.first.hexAsBigInteger(), it.second) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeForResult(onNext = { onSafeAdded() }, onError = this::onSafeAddError)

    private fun onSafeAdded() {
        snackbar(layout_transaction_details_coordinator, getString(R.string.added_multisig))
    }

    private fun onSafeAddError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_transaction_details_coordinator, getString(R.string.add_multisig_wallet_error))
    }

    // Multisig Wallet Details
    private fun safeDetailsDisposable() =
            viewModel.observeSafeDetails()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onNext = this::displaySafeInfo, onError = Timber::e)

    private fun displaySafeInfo(safe: Safe) {
        layout_transaction_details_wallet_name.text = if (safe.name.isNullOrEmpty()) getString(R.string.multisig_address) else safe.name
        layout_transaction_details_add_wallet.visibility = View.GONE
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
        when (viewModel.getTransactionType()) {
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
