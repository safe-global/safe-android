package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_transaction_details.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger


class CreateTransactionActivity : BaseTransactionActivity() {

    override fun screenId() = ScreenId.CREATE_TRANSACTION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_transaction_details_toolbar.title = getString(R.string.create_transaction)
    }

    override fun loadTransactionDetails() {
        if (!parseAndDisplayExtras(intent)) {
            toast(R.string.transaction_details_error)
            finish()
        }
    }

    private fun parseAndDisplayExtras(intent: Intent?): Boolean {
        intent ?: return false
        val safe = intent.getStringExtra(EXTRA_SAFE)
        val type = TransactionType.values().getOrNull(intent.getIntExtra(EXTRA_TYPE, -1)) ?: return false
        val transaction = intent.getParcelableExtra<TransactionParcelable>(EXTRA_TRANSACTION)?.transaction
        displayTransactionDetails(createDetailsFragment(safe, type, transaction, true))
        return true
    }

    override fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }


    companion object {

        private const val EXTRA_SAFE = "extra.string.safe"
        private const val EXTRA_TYPE = "extra.int.type"
        private const val EXTRA_TRANSACTION = "extra.parcelable.transaction"

        fun createIntent(context: Context, safeAddress: BigInteger?, type: TransactionType, transaction: Transaction?) =
                Intent(context, CreateTransactionActivity::class.java).apply {
                    putExtra(EXTRA_SAFE, safeAddress?.asEthereumAddressString())
                    putExtra(EXTRA_TYPE, type.ordinal)
                    putExtra(EXTRA_TRANSACTION, transaction?.parcelable())
                }
    }
}