package pm.gnosis.heimdall.ui.transactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_sign_transaction.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.*

class SignTransactionActivity : ViewTransactionActivity() {

    override fun screenId() = ScreenId.SIGN_TRANSACTION

    private var cachedTransactionData: CachedTransactionData? = null
    private var credentialsConfirmed: Boolean = false
    private var sendViaPush: Boolean = false

    private val signTransformer: ObservableTransformer<Pair<Solidity.Address?, Result<Transaction>>, *> =
        ObservableTransformer { up: Observable<Pair<Solidity.Address?, Result<Transaction>>> ->
            // We combine the data with the submit button events
            up.switchMap { safeWithTransaction -> layout_sign_transaction_sign_button.clicks().map { safeWithTransaction } }
                .doOnNext { (safe, transaction) ->
                    if (safe != null && transaction is DataResult) {
                        cachedTransactionData = CachedTransactionData(safe, transaction.data)
                        startActivityForResult(UnlockActivity.createConfirmIntent(this), REQUEST_CODE_CONFIRM_CREDENTIALS)
                    } else {
                        cachedTransactionData = null
                    }
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendViaPush = intent?.getBooleanExtra(EXTRA_SEND_VIA_PUSH, false) ?: false
    }

    override fun layout() = R.layout.layout_sign_transaction

    override fun toolbar(): Toolbar = layout_sign_transaction_toolbar

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        credentialsConfirmed = requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS && resultCode == Activity.RESULT_OK
    }

    override fun onStart() {
        super.onStart()
        cachedTransactionData?.let {
            if (credentialsConfirmed) {
                disposables += signTransaction(it.safeAddress, it.transaction)
                    .subscribeForResult({ (signature, qrCode) ->
                        if (qrCode == null) finish()
                        eventTracker.submit(Event.SignedTransaction())
                        layout_sign_transaction_qr_code.setImageBitmap(qrCode)
                        layout_sign_transaction_qr_code.visibility = View.VISIBLE
                        layout_sign_transaction_signature.text = signature
                        layout_sign_transaction_signature.visibility = View.VISIBLE
                        layout_sign_transaction_sign_button.visibility = View.GONE
                    }, { showErrorSnackbar(it) })
            } else {
                snackbar(layout_sign_transaction_sign_button, R.string.please_confirm_credentials)
            }
        }
        cachedTransactionData = null
        credentialsConfirmed = false
    }

    override fun fragmentRegistered() {
        layout_sign_transaction_progress_bar.visibility = View.GONE
    }

    override fun transactionDataTransformer(): ObservableTransformer<Pair<Solidity.Address?, Result<Transaction>>, Any> =
        ObservableTransformer {
            it
                .doOnNext { layout_sign_transaction_sign_button.isEnabled = it.first != null && it.second is DataResult }
                .compose(signTransformer)
        }

    private fun signTransaction(safe: Solidity.Address, transaction: Transaction) =
        viewModel.signTransaction(safe, transaction, sendViaPush)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { signingTransaction(true) }
            .doAfterTerminate { signingTransaction(false) }
            .toObservable()

    private fun signingTransaction(loading: Boolean) {
        layout_sign_transaction_progress_bar.visible(loading)
        layout_sign_transaction_sign_button.isEnabled = !loading
        transactionInputEnabled(!loading)
    }

    override fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build()
            .inject(this)
    }

    private data class CachedTransactionData(val safeAddress: Solidity.Address, val transaction: Transaction)

    companion object {
        private const val REQUEST_CODE_CONFIRM_CREDENTIALS = 2342
        private const val EXTRA_SEND_VIA_PUSH = "extra.boolean.send_via_push"

        fun createIntent(context: Context, safeAddress: Solidity.Address?, transaction: Transaction, sendViaPush: Boolean = false) =
            Intent(context, SignTransactionActivity::class.java).apply {
                putExtras(createBundle(safeAddress, transaction))
                putExtra(EXTRA_SEND_VIA_PUSH, sendViaPush)
            }
    }
}
