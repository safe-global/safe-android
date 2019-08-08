package pm.gnosis.heimdall.ui.safe.recover.extension

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.*
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirst
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.ui.tokens.payment.PaymentTokensActivity
import pm.gnosis.heimdall.utils.InfoTipDialogBuilder
import pm.gnosis.heimdall.utils.underline
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_fees_error as feesError
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_fees_info as feesInfo
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_fees_value as feesValue
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_gas_token_balance_value as balanceAfterValue
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_safe_balance_value as balanceBeforeValue
import kotlinx.android.synthetic.main.screen_replace_extension_start.replace_extension_back_arrow as backArrow
import kotlinx.android.synthetic.main.screen_replace_extension_start.replace_extension_bottom_panel as bottomPanel
import kotlinx.android.synthetic.main.screen_replace_extension_start.replace_extension_progress_bar as progressBar

class ReplaceExtensionStartActivity : ViewModelActivity<ReplaceExtensionStartContract>() {

    override fun layout() = R.layout.screen_replace_extension_start

    override fun inject(component: ViewComponent) = viewComponent().inject(this)

    override fun screenId() = ScreenId.REPLACE_BROWSER_EXTENSION_START

    private lateinit var safe: Solidity.Address


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safe = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: run {
            finish()
            return
        }

        viewModel.setup(safe)

        bottomPanel.disabled = true

        viewModel.observableState.observe(this, Observer {

            if (it.sufficient) {
                bottomPanel.disabled = false
                feesError.visibility = View.GONE
            } else {
                bottomPanel.disabled = true
                feesError.text = getString(R.string.insufficient_funds_please_add, it.tokenSymbol)
                feesError.visibility = View.VISIBLE
            }
            progressBar.visibility = View.GONE
            balanceBeforeValue.text = it.balanceBefore
            feesValue.text = it.fee.underline()
            balanceAfterValue.text = it.balanceAfter

        })

    }

    override fun onStart() {
        super.onStart()

        backArrow.setOnClickListener {
            finish()
        }

        feesValue.setOnClickListener {
            startActivity(PaymentTokensActivity.createIntent(this, safe))
        }

        disposables += feesInfo.clicks()
            .subscribeBy {
                InfoTipDialogBuilder.build(this, R.layout.dialog_network_fee, R.string.ok).show()
            }

        disposables += bottomPanel.forwardClicks.subscribe {
            startActivity(ReplaceExtensionQrActivity.createIntent(this, safe))
        }

        viewModel.estimate()
    }


    companion object {

        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) = Intent(context, ReplaceExtensionStartActivity::class.java).apply {
            putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
        }
    }
}


abstract class ReplaceExtensionStartContract : ViewModel() {

    abstract val observableState: LiveData<ViewUpdate>

    abstract fun setup(safeAddress: Solidity.Address)

    abstract fun estimate()


    data class ViewUpdate(
        val balanceBefore: String,
        val fee: String,
        val balanceAfter: String,
        val tokenSymbol: String,
        val sufficient: Boolean
    )
}


class ReplaceExtensionStartViewModel @Inject constructor(
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val tokenRepository: TokenRepository,
    private val transactionExecutionRepository: TransactionExecutionRepository

) : ReplaceExtensionStartContract() {

    override val observableState: LiveData<ViewUpdate>
        get() = _state
    private val _state = MutableLiveData<ViewUpdate>()

    private lateinit var safeAddress: Solidity.Address

    override fun setup(safeAddress: Solidity.Address) {
        this.safeAddress = safeAddress
    }

    override fun estimate() {

        viewModelScope.launch {

            val safeInfo = gnosisSafeRepository.loadInfo(safeAddress).awaitFirst()
            val paymentToken = tokenRepository.loadPaymentToken(safeAddress).await()


            val balance = tokenRepository.loadTokenBalances(safeAddress, listOf(paymentToken))
                .repeatWhen { it.delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS) }
                .retryWhen { it.delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS) }
                .awaitFirst()[0]


            val safeOwners = safeInfo.owners.subList(1, safeInfo.owners.size)
            val transaction = recoverSafeOwnersHelper.buildRecoverTransaction(
                safeInfo,
                safeOwners.toSet(),
                setOf(Solidity.Address(safeInfo.owners[0].value.add(1.toBigInteger())))
            )
            val executeInfo = transactionExecutionRepository.loadExecuteInformation(safeAddress, paymentToken.address, transaction).await()


            val currentBalance = balance.second!!
            val gasFee = with(executeInfo) {
                (txGas + dataGas + operationalGas) * gasPrice
            }
            _state.postValue(
                ViewUpdate(
                    paymentToken.displayString(currentBalance),
                    ERC20TokenWithBalance(paymentToken, gasFee).displayString(roundingMode = RoundingMode.UP),
                    paymentToken.displayString(currentBalance - gasFee),
                    paymentToken.symbol,
                    currentBalance > gasFee

                )
            )

            Timber.d(executeInfo.gasPrice.toString())
        }
    }

    companion object {
        private const val BALANCE_REQUEST_INTERVAL_SECONDS = 10L
    }
}








