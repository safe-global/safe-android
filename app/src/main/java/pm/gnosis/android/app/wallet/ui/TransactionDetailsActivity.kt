package pm.gnosis.android.app.wallet.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_transaction_details.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.model.Transaction
import pm.gnosis.android.app.wallet.data.model.TransactionJson
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule

class TransactionDetailsActivity : AppCompatActivity() {
    companion object {
        const val TRANSACTION_EXTRA = "extra.transaction"
    }

    private var transaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_transaction_details)

        intent.extras?.let {
            val transactionJson: TransactionJson = it.getParcelable(TRANSACTION_EXTRA)
            transaction = transactionJson.read()
        }
        fillTransactionDetails()
    }

    fun fillTransactionDetails() {
        recipient.text = transaction?.to ?: "No value"
        gas_limit.text = transaction?.gasLimit.toString() ?: "No value"
        gas_price.text = transaction?.gasPrice.toString() ?: "No value"
        nonce.text = transaction?.nonce ?: "No value"
        value.text = transaction?.value.toString() ?: "No value"
        data.text = transaction?.data ?: "No value"
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }
}