package pm.gnosis.android.app.wallet.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_transaction_details.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.model.TransactionDetails
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule

class TransactionDetailsActivity : AppCompatActivity() {
    companion object {
        const val TRANSACTION_EXTRA = "extra.transaction"
    }

    private var transaction: TransactionDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_transaction_details)

        intent.extras?.let {
            transaction = it.getParcelable(TRANSACTION_EXTRA)
        }
        fillTransactionDetails()
    }

    fun fillTransactionDetails() {
        recipient.text = transaction?.address?.let { "0x${it.toString(16)}" } ?: "No value"
        suggested_gas.text = transaction?.gas?.let { it.toString(10) } ?: "No value"
        value.text = transaction?.value?.let { it.toEther().stripTrailingZeros().toPlainString() } ?: "No value"
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
