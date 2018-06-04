package pm.gnosis.heimdall.ui.transactions.create


import android.content.Context
import android.content.Intent
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_create_asset_transfer.*
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferContract.ViewUpdate
import pm.gnosis.heimdall.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.stringWithNoTrailingZeroes
import javax.inject.Inject

class CreateAssetTransferActivity : ViewModelActivity<CreateAssetTransferContract>() {

    @Inject
    lateinit var addressHelper: AddressHelper

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.CREATE_ASSET_TRANSFER

    override fun layout() = R.layout.layout_create_asset_transfer

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        disposables += Single.fromCallable {
            var address: Solidity.Address? = null
            handleQrCodeActivityResult(requestCode, resultCode, data, {
                address = parseEthereumAddress(it) ?: throw IllegalArgumentException()
            })

            // We couldn't parse an address yet
            if (address == null) {
                handleAddressBookResult(requestCode, resultCode, data, {
                    address = it.address
                })
            }
            address?.asEthereumAddressChecksumString() ?: ""
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::onAddressProvided, onError = { toast(R.string.invalid_ethereum_address) })

    }

    private fun onAddressProvided(address: String) {
        if (!address.isBlank()) {
            layout_create_asset_transfer_input_receiver.setText(address)
            layout_create_asset_transfer_input_receiver.setSelection(address.length)
            layout_create_asset_transfer_input_receiver.setTextColor(getColorCompat(R.color.dark_slate_blue))
        }
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
        val reviewEvents = layout_create_asset_transfer_continue_button.clicks()
        disposables +=
                Observable.combineLatest(
                    layout_create_asset_transfer_input_value.textChanges()
                        .doOnNext {
                            layout_create_asset_transfer_input_value.setTextColor(getColorCompat(R.color.dark_slate_blue))
                        },
                    layout_create_asset_transfer_input_receiver.textChanges()
                        .doOnNext {
                            layout_create_asset_transfer_input_receiver.setTextColor(getColorCompat(R.color.dark_slate_blue))
                        },
                    BiFunction { value: CharSequence, receiver: CharSequence ->
                        CreateAssetTransferContract.Input(value.toString(), receiver.toString())
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

        disposables +=
                layout_create_asset_transfer_qr_code.clicks()
                    .subscribeBy(onNext = {
                        QRCodeScanActivity.startForResult(this)
                    })

        disposables +=
                layout_create_asset_transfer_address_book.clicks()
                    .subscribeBy(onNext = {
                        selectFromAddressBook()
                    })

        disposables += layout_create_asset_transfer_back_button.clicks()
            .subscribeBy { onBackPressed() }

        addressHelper.populateAddressInfo(
            layout_create_asset_transfer_safe_address,
            layout_create_asset_transfer_safe_name,
            layout_create_asset_transfer_safe_image,
            safe
        ).forEach { disposables += it }

        disposables += toolbarHelper.setupShadow(layout_create_asset_transfer_toolbar_shadow, layout_create_asset_transfer_title_content_scroll)
    }

    private fun applyUpdate(update: ViewUpdate) {
        when (update) {
            is CreateAssetTransferContract.ViewUpdate.Estimate -> {
                layout_create_asset_transfer_balance_value.text =
                        getString(R.string.x_ether, update.balance.toEther().stringWithNoTrailingZeroes())
                layout_create_asset_transfer_fees_value.text =
                        "- ${getString(R.string.x_ether, update.estimate.toEther().stringWithNoTrailingZeroes())}"
                layout_create_asset_transfer_continue_button.visible(update.canExecute)
                if (!update.canExecute) layout_create_asset_transfer_input_value.setTextColor(getColorCompat(R.color.tomato))
                else {
                    layout_create_asset_transfer_input_receiver.setTextColor(getColorCompat(R.color.dark_slate_blue))
                    layout_create_asset_transfer_input_value.setTextColor(getColorCompat(R.color.dark_slate_blue))
                }
            }
            CreateAssetTransferContract.ViewUpdate.EstimateError -> disableContinue()
            is CreateAssetTransferContract.ViewUpdate.TokenInfo -> {
                update.value.token.name?.let {
                    layout_create_asset_transfer_title.text = getString(R.string.transfer_x, it)
                }
                layout_create_asset_transfer_safe_balance.text = update.value.displayString()
                layout_create_asset_transfer_input_label.text = update.value.token.symbol ?: "???"
            }
            is CreateAssetTransferContract.ViewUpdate.InvalidInput -> {
                layout_create_asset_transfer_input_value.setTextColor(
                    getColorCompat(
                        if (update.amount) R.color.tomato else R.color.dark_slate_blue
                    )
                )
                layout_create_asset_transfer_input_receiver.setTextColor(
                    getColorCompat(
                        if (update.address) R.color.tomato else R.color.dark_slate_blue
                    )
                )
            }
            is CreateAssetTransferContract.ViewUpdate.StartReview -> {
                startActivity(update.intent)
            }
        }
    }

    private fun disableContinue() {
        layout_create_asset_transfer_balance_value.text = "-"
        layout_create_asset_transfer_fees_value.text = "-"
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
