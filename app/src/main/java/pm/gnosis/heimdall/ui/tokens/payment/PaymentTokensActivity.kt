package pm.gnosis.heimdall.ui.tokens.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_payment_tokens.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.weak
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.lang.ref.WeakReference
import javax.inject.Inject

class PaymentTokensActivity : ViewModelActivity<PaymentTokensContract>() {
    @Inject
    lateinit var adapter: PaymentTokensAdapter

    private var weakSnackbar: WeakReference<Snackbar>? = null

    override fun layout() = R.layout.layout_payment_tokens

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId(): ScreenId = ScreenId.SELECT_PAYMENT_TOKEN

    private var nextAction: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payment_tokens_recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        payment_tokens_recycler_view.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        payment_tokens_recycler_view.adapter = adapter

        nextAction = intent.getParcelableExtra<Intent?>(EXTRA_NEXT_ACTION)
        val safe = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.let { it.asEthereumAddress()!! }
        val transaction = intent.getParcelableExtra<SafeTransaction>(EXTRA_TRANSACTION)
        val metricType = when {
            safe == null -> PaymentTokensContract.MetricType.CreationFees(intent.getLongExtra(EXTRA_OWNER_COUNT, 4))
            transaction != null -> PaymentTokensContract.MetricType.TransactionFees(safe, transaction)
            else -> PaymentTokensContract.MetricType.Balance(safe)
        }
        viewModel.setup(metricType)
        viewModel.state.observe(this, Observer { state ->
            payment_tokens_swipe_refresh.isRefreshing = state.loading
            adapter.submitList(state.items)
            hidePendingSnackbar()
            state.viewAction?.let { performAction(it) }
        })

        val hint = intent.getStringExtra(EXTRA_HINT) ?: getString(R.string.this_payment_will_be_used)
        payment_tokens_explainer_lbl.text = hint

        payment_tokens_back_arrow.setOnClickListener { onBackPressed() }
        payment_tokens_swipe_refresh.setOnRefreshListener { refreshList() }
        payment_tokens_header_metric_lbl.text = getString(
            when (metricType) {
                is PaymentTokensContract.MetricType.Balance ->
                    R.string.balance
                is PaymentTokensContract.MetricType.TransactionFees, is PaymentTokensContract.MetricType.CreationFees ->
                    R.string.fee
            }
        )

        if (nextAction != null) {
            payment_token_bottom_panel.visible(true)
            viewModel.paymentToken.observe(this, Observer {
                payment_token_bottom_panel.forwardLabel = getString(R.string.pay_with, it.symbol)
            })
        }
    }

    override fun onStart() {
        super.onStart()
        nextAction?.let { nextAction ->
            disposables += payment_token_bottom_panel.forwardClicks.subscribeBy {
                startActivity(nextAction)
            }
        }
    }

    private fun performAction(viewAction: PaymentTokensContract.ViewAction): Any =
        when (viewAction) {
            is PaymentTokensContract.ViewAction.ShowError ->
                weakSnackbar = weak(errorSnackbar(
                    payment_tokens_recycler_view,
                    viewAction.error,
                    duration = Snackbar.LENGTH_INDEFINITE,
                    action = getString(R.string.retry) to { _: View ->
                        refreshList()
                    }
                ))
        }

    private fun refreshList() {
        hidePendingSnackbar()
        viewModel.loadPaymentTokens()
    }

    private fun hidePendingSnackbar() {
        weakSnackbar?.get()?.dismiss()
        weakSnackbar = null
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_TRANSACTION = "extra.parcelable.transaction"
        private const val EXTRA_OWNER_COUNT = "extra.integer.owner_count"
        private const val EXTRA_HINT = "extra.string.hint"
        private const val EXTRA_NEXT_ACTION = "extra.parcelable.next_action"

        fun createIntent(
            context: Context,
            safeAddress: Solidity.Address? = null,
            transaction: SafeTransaction? = null,
            ownerCount: Long? = null,
            hint: String? = null,
            nextAction: Intent? = null) =
            Intent(context, PaymentTokensActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress?.asEthereumAddressString())
                putExtra(EXTRA_OWNER_COUNT, ownerCount)
                putExtra(EXTRA_TRANSACTION, transaction)
                putExtra(EXTRA_HINT, hint)
                putExtra(EXTRA_NEXT_ACTION, nextAction)
            }
    }
}
