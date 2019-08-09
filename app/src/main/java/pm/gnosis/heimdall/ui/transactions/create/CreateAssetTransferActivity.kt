package pm.gnosis.heimdall.ui.transactions.create


import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.layout_create_asset_transfer.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.AddressInputHelper
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.tokens.payment.PaymentTokensActivity
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferContract.ViewUpdate
import pm.gnosis.heimdall.utils.InfoTipDialogBuilder
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.underline
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class CreateAssetTransferActivity : ViewModelActivity<CreateAssetTransferContract>() {

    @Inject
    lateinit var addressHelper: AddressHelper

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    @Inject
    lateinit var picasso: Picasso

    private val receiverInputSubject = BehaviorSubject.create<Solidity.Address>()

    private var paymentToken: ERC20Token? = null

    private lateinit var addressInputHelper: AddressInputHelper

    override fun screenId() = ScreenId.TRANSACTION_ENTER_DATA

    override fun layout() = R.layout.layout_create_asset_transfer

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        addressInputHelper.handleResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addressInputHelper = AddressInputHelper(this, ::handleNewAddress)
    }

    private fun handleNewAddress(address: Solidity.Address) {
        layout_create_asset_transfer_receiver_hint.text = null
        layout_create_asset_transfer_receiver_name.visible(false)
        addressHelper.populateAddressInfo(
            layout_create_asset_transfer_receiver_address,
            layout_create_asset_transfer_receiver_name,
            layout_create_asset_transfer_receiver_image,
            address
        ).forEach { disposables += it }
        receiverInputSubject.onNext(address)
    }

    override fun onStart() {
        super.onStart()

        val safe = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: run {
            finish()
            return
        }

        val token = intent.getStringExtra(EXTRA_TOKEN_ADDRESS)?.asEthereumAddress() ?: run {
            finish()
            return
        }

        disableContinue()
        val reviewEvents = layout_create_asset_transfer_continue_button.clicks()
        disposables +=
            Observable.combineLatest(
                layout_create_asset_transfer_input_value.textChanges()
                    .doOnNext {
                        layout_create_asset_transfer_input_value.setTextColor(getColorCompat(R.color.blue))
                    },
                receiverInputSubject,
                BiFunction { value: CharSequence, receiver: Solidity.Address ->
                    CreateAssetTransferContract.Input(value.toString(), receiver)
                }
            )
                .doOnNext { disableContinue() }
                .compose(viewModel.processInput(safe, token, reviewEvents))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(
                    onNext = ::applyUpdate,
                    onError = {
                        errorSnackbar(layout_create_asset_transfer_continue_button, it)
                    }
                )

        disposables += layout_create_asset_transfer_back_button.clicks()
            .subscribeBy { onBackPressed() }

        disposables += layout_create_asset_transfer_receiver_hint.clicks()
            .subscribeBy { addressInputHelper.showDialog() }

        disposables += layout_create_asset_transfer_fees_info.clicks()
            .subscribeBy {
                InfoTipDialogBuilder.build(this, R.layout.dialog_network_fee, R.string.ok).show()
            }

        disposables += viewModel.loadPaymentToken(safe)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = {
                Timber.e(it)
                updatePaymentToken(null)
            }) {
                updatePaymentToken(it)
            }

        disposables += layout_create_asset_transfer_fees_value.clicks()
            .mergeWith(layout_create_asset_transfer_fees_settings.clicks())
            .subscribeBy {
                startActivity(PaymentTokensActivity.createIntent(this, safe))
            }

        addressHelper.populateAddressInfo(
            layout_create_asset_transfer_safe_address,
            layout_create_asset_transfer_safe_name,
            layout_create_asset_transfer_safe_image,
            safe
        ).forEach { disposables += it }

        disposables += toolbarHelper.setupShadow(layout_create_asset_transfer_toolbar_shadow, layout_create_asset_transfer_title_content_scroll)
    }

    private fun updatePaymentToken(token: ERC20Token?) {
        // Clear if previous token
        if (layout_create_asset_transfer_fees_value.text.toString() == paymentToken?.symbol)
            layout_create_asset_transfer_fees_value.text = null
        paymentToken = token
        // Set new token if no estimate
        if (layout_create_asset_transfer_fees_value.text.isEmpty()) {
            layout_create_asset_transfer_fees_value.text = token?.symbol?.underline()
        }
    }

    private fun applyUpdate(update: ViewUpdate) {
        when (update) {
            is ViewUpdate.Estimate -> {
                layout_create_asset_transfer_gas_token_balance_after_transfer_value.text =
                    update.gasToken.displayString()
                layout_create_asset_transfer_fees_value.text = update.gasToken.token.displayString(update.networkFee).underline()
                layout_create_asset_transfer_continue_button.visible(update.sufficientFunds)
                layout_create_asset_transfer_fees_error.visible(!update.sufficientFunds)

                if (update.assetBalanceAfterTransfer != null) {
                    layout_create_asset_transfer_asset_balance_after_transfer_value.setTextColor(getColorCompat(R.color.battleship_grey))
                    layout_create_asset_transfer_asset_balance_after_transfer_value.text = update.assetBalanceAfterTransfer.displayString()
                    layout_create_asset_transfer_asset_balance_after_transfer_label.visible(true)
                    layout_create_asset_transfer_asset_balance_after_transfer_value.visible(true)
                } else {
                    layout_create_asset_transfer_gas_token_balance_after_transfer_label.setTextColor(getColorCompat(R.color.blue))
                    layout_create_asset_transfer_asset_balance_after_transfer_label.visible(false)
                    layout_create_asset_transfer_asset_balance_after_transfer_value.visible(false)
                }

                if (!update.sufficientFunds) layout_create_asset_transfer_input_value.setTextColor(getColorCompat(R.color.tomato))
                else {
                    layout_create_asset_transfer_input_value.setTextColor(getColorCompat(R.color.blue))
                }
            }
            is ViewUpdate.EstimateError -> {
                disableContinue()
                errorSnackbar(layout_create_asset_transfer_input_label, update.error)
            }
            is ViewUpdate.TokenInfo -> {
                layout_create_asset_transfer_title.text = getString(R.string.send_x, update.value.token.name)
                layout_create_asset_transfer_safe_balance.text = update.value.displayString()
                layout_create_asset_transfer_input_label.text = update.value.token.symbol
                layout_create_asset_transfer_input_icon.setImageDrawable(null)
                if (update.value.token == ETHER_TOKEN)
                    layout_create_asset_transfer_input_icon.setImageResource(R.drawable.ic_ether_symbol)
                else if (update.value.token.logoUrl.isBlank())
                    picasso.load(update.value.token.logoUrl).into(layout_create_asset_transfer_input_icon)
            }
            is ViewUpdate.InvalidInput -> {
                layout_create_asset_transfer_input_value.setTextColor(
                    getColorCompat(
                        if (update.amount) R.color.tomato else R.color.blue
                    )
                )
            }
            is ViewUpdate.StartReview -> {
                startActivity(update.intent)
            }
        }
    }

    private fun disableContinue() {
        layout_create_asset_transfer_asset_balance_after_transfer_label.visible(false)
        layout_create_asset_transfer_asset_balance_after_transfer_value.visible(false)
        layout_create_asset_transfer_fees_error.visible(false)
        layout_create_asset_transfer_gas_token_balance_after_transfer_value.text = "-"
        layout_create_asset_transfer_fees_value.text = paymentToken?.symbol?.underline()
        layout_create_asset_transfer_continue_button.visible(false)
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_TOKEN_ADDRESS = "extra.string.token_address"
        fun createIntent(context: Context, safe: Solidity.Address, token: ERC20Token) =
            Intent(context, CreateAssetTransferActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safe.asEthereumAddressString())
                putExtra(EXTRA_TOKEN_ADDRESS, token.address.asEthereumAddressString())
            }
    }
}
