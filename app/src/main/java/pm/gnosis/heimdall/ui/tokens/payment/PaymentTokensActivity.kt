package pm.gnosis.heimdall.ui.tokens.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_payment_tokens.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class PaymentTokensActivity : ViewModelActivity<PaymentTokensContract>() {
    @Inject
    lateinit var adapter: PaymentTokensAdapter

    override fun layout() = R.layout.layout_payment_tokens

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId(): ScreenId = ScreenId.SELECT_PAYMENT_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payment_tokens_recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        payment_tokens_recycler_view.adapter = adapter

        val safe = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.let { it.asEthereumAddress()!! }
        /*
        val transaction = intent.getParcelableExtra<SafeTransaction>(EXTRA_TRANSACTION)
        val metricType = when {
            safe == null -> PaymentTokensContract.MetricType.CreationFees(2)
            transaction != null -> PaymentTokensContract.MetricType.TransactionFees(transaction)
            else -> PaymentTokensContract.MetricType.Balance
        }
        */
        viewModel.setup(safe, PaymentTokensContract.MetricType.Balance)
        viewModel.state.observe(this, Observer { state ->
            payment_tokens_swipe_refresh.isRefreshing = state.loading
            adapter.submitList(state.items)
            state.viewAction?.let { performAction(it) }
        })

        payment_tokens_back_arrow.setOnClickListener { onBackPressed() }
        payment_tokens_swipe_refresh.setOnRefreshListener { viewModel.loadPaymentTokens() }
    }

    private fun performAction(viewAction: PaymentTokensContract.ViewAction): Any =
        when(viewAction) {
            is PaymentTokensContract.ViewAction.ShowError -> errorSnackbar(payment_tokens_recycler_view, viewAction.error)
        }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_TRANSACTION = "extra.parcelable.transaction"
        fun createIntent(context: Context, safeAddress: Solidity.Address?, transaction: SafeTransaction? = null) =
            Intent(context, PaymentTokensActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress?.asEthereumAddressString())
                putExtra(EXTRA_TRANSACTION, transaction)
            }
    }
}
