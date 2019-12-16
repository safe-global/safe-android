package pm.gnosis.heimdall.ui.safe.upgrade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.android.synthetic.main.layout_upgrade_master_copy.*
import kotlinx.coroutines.*
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirst
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.tokens.payment.PaymentTokensActivity
import pm.gnosis.heimdall.ui.transactions.builder.UpdateMasterCopyTransactionBuilder
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.heimdall.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

abstract class UpgradeMasterCopyContract(
    context: Context,
    appDispatcher: ApplicationModule.AppCoroutineDispatchers
) : BaseStateViewModel<UpgradeMasterCopyContract.State>(context, appDispatcher) {

    abstract fun setup(safe: Solidity.Address)

    abstract fun observe(lifecycleOwner: LifecycleOwner, update: (State) -> Unit)

    abstract fun loadEstimates()

    abstract fun changeFeeToken()

    abstract fun next()

    data class State(
        val safeName: String?,
        val feeToken: ERC20Token?,
        val fees: BigInteger?,
        val safeBalance: BigInteger?,
        val safeBalanceAfterTx: BigInteger?,
        val showFeeError: Boolean,
        val loading: Boolean,
        override var viewAction: ViewAction?
    ) : BaseStateViewModel.State
}

class UpgradeMasterCopyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    appDispatcher: ApplicationModule.AppCoroutineDispatchers,
    private val addressBookRepository: AddressBookRepository,
    private val executionRepository: TransactionExecutionRepository,
    private val safeRepository: GnosisSafeRepository,
    private val tokenRepository: TokenRepository
) : UpgradeMasterCopyContract(context, appDispatcher) {
    override val state = liveData {
        loadSafeName()
        for (state in stateChannel.openSubscription()) emit(state)
    }

    private val loadingErrorHandler = CoroutineExceptionHandler { context, e ->
        viewModelScope.launch { updateState { copy(loading = false) } }
        coroutineErrorHandler.handleException(context, e)
    }
    private fun loadingLaunch(block: suspend CoroutineScope.() -> Unit) = safeLaunch(loadingErrorHandler, block)

    private lateinit var safe: Solidity.Address
    override fun setup(safe: Solidity.Address) {
        this.safe = safe
    }

    private fun loadSafeName() {
        safeLaunch {
            val safeName = addressBookRepository.loadAddressBookEntry(safe).await().name
            updateState { copy(safeName = safeName) }
        }
    }

    private var transactionData: TransactionData.UpdateMasterCopy? = null

    private var estimatesJob: Job? = null
    override fun loadEstimates() {
        // Check if already loading
        if (estimatesJob?.isActive == true) return
        estimatesJob = loadingLaunch {
            transactionData = null
            updateState { copy(loading = true, feeToken = null, fees = null, safeBalance = null, safeBalanceAfterTx = null, showFeeError = false) }
            val paymentToken = tokenRepository.loadPaymentToken(safe).await()
            updateState { copy(feeToken = paymentToken) }
            val (masterCopy) = safeRepository.checkSafe(safe).awaitFirst()
            val newMasterCopy = SafeContractUtils.checkForUpdate(masterCopy)
            check(newMasterCopy != null) { "No upgrade possible!" }
            val data = TransactionData.UpdateMasterCopy(newMasterCopy)
            val transaction = UpdateMasterCopyTransactionBuilder.build(safe, data)
            val estimate = executionRepository.loadExecuteInformation(safe, paymentToken.address, transaction).await()
            val safeBalance = estimate.balance
            val fees = estimate.gasCosts()
            val balanceAfterTx = safeBalance - fees
            // Master copy update could be estimated, therefore we should cache the data
            transactionData = data
            updateState {
                copy(loading = false, fees = fees, safeBalance = safeBalance, safeBalanceAfterTx = balanceAfterTx, showFeeError = fees > safeBalance)
            }
        }
    }

    private val lifecycleObserver by lazy { LifecycleBoundObserver(onStart = ::loadEstimates) }

    override fun observe(lifecycleOwner: LifecycleOwner, update: (State) -> Unit) {
        state.observe(lifecycleOwner, Observer { update(it) })
        lifecycleObserver.observe(lifecycleOwner)
    }

    override fun next() {
        safeLaunch {
            transactionData?.let { data ->
                updateState { copy(viewAction = ViewAction.StartActivity(ReviewTransactionActivity.createIntent(context, safe, data))) }
            }
        }
    }

    override fun changeFeeToken() {
        safeLaunch {
            updateState { copy(viewAction = ViewAction.StartActivity(PaymentTokensActivity.createIntent(context, safe))) }
        }
    }

    override fun initialState() = State(null, null, null, null, null, false, false, null)
}

class UpgradeMasterCopyActivity : ViewModelActivity<UpgradeMasterCopyContract>() {

    override fun screenId() = ScreenId.UPGRADE_MASTER_COPY

    override fun layout() = R.layout.layout_upgrade_master_copy

    override fun inject(component: ViewComponent) = component.inject(this)

    private val paymentTokenClickListener = View.OnClickListener { viewModel.changeFeeToken() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setup(intent.getStringExtra(EXTRA_SAFE)?.asEthereumAddress()!!)
        viewModel.observe(this, ::onUpdate)
        upgrade_master_copy_back_button.setOnClickListener { onBackPressed() }
        upgrade_master_copy_next.setOnClickListener { viewModel.next() }
        upgrade_master_copy_fees_info.setOnClickListener { InfoTipDialogBuilder.build(this, R.layout.dialog_network_fee, R.string.ok).show() }
        upgrade_master_copy_fees_value.setOnClickListener(paymentTokenClickListener)
        upgrade_master_copy_fees_settings.setOnClickListener(paymentTokenClickListener)
        upgrade_master_copy_refresh.setOnRefreshListener { viewModel.loadEstimates() }

        upgrade_master_copy_next.setCompoundDrawableResource(right = R.drawable.ic_arrow_forward_24dp)
    }

    private fun onUpdate(state: UpgradeMasterCopyContract.State) {
        upgrade_master_copy_fee_explainer.text = getString(R.string.this_will_upgrade, state.safeName ?: getString(R.string.default_safe_name))
        upgrade_master_copy_fees_error.visible(state.showFeeError)
        val canUpgrade = state.fees != null && !state.showFeeError
        upgrade_master_copy_refresh.isRefreshing = state.loading
        upgrade_master_copy_bottom_bar.isEnabled = canUpgrade
        upgrade_master_copy_next.isEnabled = canUpgrade
        upgrade_master_copy_safe_balance_after_transfer_value.setTextColor(getColorCompat(if (state.showFeeError) R.color.tomato else R.color.dark_grey))
        state.feeToken?.let { token ->
            upgrade_master_copy_fees_error.text = getString(R.string.insufficient_funds_please_add_tokenpayment, token.symbol)
            upgrade_master_copy_fees_value.text = state.fees?.let { token.displayString(it) } ?: token.symbol
            upgrade_master_copy_safe_balance_value.text = state.safeBalance?.let { token.displayString(it) } ?: "-"
            upgrade_master_copy_safe_balance_after_transfer_value.text = state.safeBalanceAfterTx?.let { token.displayString(it) } ?: "-"
        } ?: run {
            upgrade_master_copy_fees_error.text = getString(R.string.insufficient_funds)
            upgrade_master_copy_fees_value.text = "-"
            upgrade_master_copy_safe_balance_value.text = "-"
            upgrade_master_copy_safe_balance_after_transfer_value.text = "-"
        }
        when (val action = state.viewAction) {
            is BaseStateViewModel.ViewAction.ShowError -> errorSnackbar(upgrade_master_copy_bottom_bar, action.error)
            is BaseStateViewModel.ViewAction.StartActivity -> startActivity(action.intent)
        }
    }

    companion object {
        private const val EXTRA_SAFE = "extra.string.safe"
        fun createIntent(context: Context, safe: Solidity.Address) = Intent(context, UpgradeMasterCopyActivity::class.java).apply {
            putExtra(EXTRA_SAFE, safe.asEthereumAddressString())
        }
    }
}
