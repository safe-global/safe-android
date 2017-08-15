package pm.gnosis.android.app.authenticator.ui.transactiondetails

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_transaction_details.*
import pm.gnosis.android.app.authenticator.GnosisAuthenticatorApplication
import pm.gnosis.android.app.authenticator.R
import pm.gnosis.android.app.authenticator.data.contracts.GnosisMultisigWrapper
import pm.gnosis.android.app.authenticator.data.db.MultisigWallet
import pm.gnosis.android.app.authenticator.data.model.TransactionDetails
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

        if (t.data.isSolidityMethod(GnosisMultisigWrapper.CONFIRM_TRANSACTION_METHOD_ID)) {
            activity_transaction_details_button.text = "Confirm Transaction"
            activity_transaction_details_transaction_id.text = GnosisMultisigWrapper.decodeConfirm(t.data)?.asDecimalString() ?: "-"
        } else if (t.data.isSolidityMethod(GnosisMultisigWrapper.REVOKE_TRANSACTION_METHOD_ID)) {
            activity_transaction_details_button.text = "Revoke Transaction"
            activity_transaction_details_transaction_id.text = GnosisMultisigWrapper.decodeRevoke(t.data)?.asDecimalString() ?: "-"
        } else {
            activity_transaction_details_button.visibility = View.GONE
        }
        activity_transaction_details_wallet_address.text = t.address.asEthereumAddressString()
    }

    override fun onStart() {
        super.onStart()
        activity_transaction_details_button.setOnClickListener {
            disposables += signTransactionDisposable()
        }

        presenter.getMultisigWalletDetails(transaction.address)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onMultisigWallet, onError = this::onMultisigWalletError)
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
