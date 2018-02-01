package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.content.Intent
import android.support.v7.widget.Toolbar
import android.view.View
import io.reactivex.ObservableTransformer
import kotlinx.android.synthetic.main.layout_receipt_transaction.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.models.Transaction
import java.math.BigInteger

class ReceiptTransactionActivity : ViewTransactionActivity() {

    override fun screenId() = ScreenId.RECEIPT_TRANSACTION

    override fun layout() = R.layout.layout_receipt_transaction

    override fun toolbar(): Toolbar = layout_receipt_transaction_toolbar

    override fun navIcon(): Int = R.drawable.ic_arrow_back_24dp

    override fun transactionDataTransformer(): ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, Any> =
            ObservableTransformer {
                it.map { Any() }
            }

    override fun fragmentRegistered() {
        layout_receipt_transaction_progress_bar.visibility = View.GONE
    }


    override fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }

    companion object {

        fun createIntent(context: Context, safeAddress: BigInteger?, transaction: Transaction) =
                Intent(context, ReceiptTransactionActivity::class.java).apply {
                    putExtras(createBundle(safeAddress, transaction))
                }
    }

}
