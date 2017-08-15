package pm.gnosis.android.app.wallet.ui.transactiondetails

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_transaction_details.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.contracts.GnosisMultisig
import pm.gnosis.android.app.wallet.data.model.TransactionDetails
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.util.asDecimalString
import pm.gnosis.android.app.wallet.util.isSolidityMethod
import pm.gnosis.android.app.wallet.util.snackbar
import pm.gnosis.android.app.wallet.util.toast
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
        if (t == null || t.data == null) {
            finish()
            return
        } else {
            transaction = t
        }

        if (t.data.isSolidityMethod(GnosisMultisig.CONFIRM_TRANSACTION_METHOD_ID)) {
            activity_transaction_details_button.text = "Confirm Transaction"
            activity_transaction_details_transaction_id.text = GnosisMultisig.decodeConfirm(t.data)?.asDecimalString() ?: "-"
        } else if (t.data.isSolidityMethod(GnosisMultisig.REVOKE_TRANSACTION_METHOD_ID)) {
            activity_transaction_details_button.text = "Revoke Transaction"
            activity_transaction_details_transaction_id.text = GnosisMultisig.decodeRevoke(t.data)?.asDecimalString() ?: "-"
        } else {
            activity_transaction_details_button.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        activity_transaction_details_button.setOnClickListener {
            disposables += signTransactionDisposable()
        }
    }

    private fun signTransactionDisposable() = presenter.signTransaction(transaction)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = this::onSendTransaction, onError = this::onSendTransactionError)

    private fun onSendTransaction(transactionHash: String) {
        transaction.data?.let {
            if (it.isSolidityMethod(GnosisMultisig.CONFIRM_TRANSACTION_METHOD_ID)) {
                toast("Transaction confirmed")
            } else if (it.isSolidityMethod(GnosisMultisig.REVOKE_TRANSACTION_METHOD_ID)) {
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
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }
}
