package pm.gnosis.heimdall.ui.transactions.create


import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_create_asset_transfer.*
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.transactions.builder.AssetTransferTransactionBuilder
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferContract.ViewUpdate
import pm.gnosis.heimdall.ui.transactions.review.ReviewTransactionActivity
import pm.gnosis.heimdall.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.removeHexPrefix
import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

class CreateAssetTransferViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val executionRepository: TransactionExecutionRepository,
    private val tokenRepository: TokenRepository
) : CreateAssetTransferContract() {
    override fun processInput(
        safe: Solidity.Address,
        tokenAddress: Solidity.Address,
        reviewEvents: Observable<Unit>
    ) = ObservableTransformer<Input, Result<ViewUpdate>> { input ->
        input.publish {
            // We parse the input and load the token information
            Observable.combineLatest(
                it.flatMap(::parseInput),
                loadTokenInfo(safe, tokenAddress),
                BiFunction { checkedInput: Pair<Solidity.Address?, BigDecimal?>, token: Optional<ERC20TokenWithBalance> ->
                    checkedInput to token.toNullable()
                }
            )
                .switchMap<Result<ViewUpdate>> { (input, token) ->
                    val updates = mutableListOf<Observable<Result<ViewUpdate>>>()
                    val (address, value) = input
                    token?.let {
                        updates += Observable.just<Result<ViewUpdate>>(DataResult(ViewUpdate.TokenInfo(it)))
                    }
                    if (address == null || value == null) {
                        updates += Observable.just<Result<ViewUpdate>>(
                            DataResult(ViewUpdate.InvalidInput(value == null, address == null))
                        )
                    } else if (token != null) {
                        updates += setupTokenCheck(safe, token, address, value, reviewEvents)
                    }
                    Observable.concat(updates)
                }
        }

    }

    private fun parseInput(input: Input): Observable<Pair<Solidity.Address?, BigDecimal?>> =
        Observable.fromCallable {
            // Address needs to be completely entered
            val address = if (input.address.removeHexPrefix().length != 40) null else input.address.asEthereumAddress()
            val amount = input.amount.toBigDecimalOrNull()
            // Value should not be zero
            address to if (amount != BigDecimal.ZERO) amount else null
        }

    private fun loadTokenInfo(safe: Solidity.Address, tokenAddress: Solidity.Address) =
        (if (tokenAddress == ERC20Token.ETHER_TOKEN.address) Single.just(ERC20Token.ETHER_TOKEN)
        else tokenRepository.loadToken(tokenAddress))
            .emitAndNext(
                emit = { ERC20TokenWithBalance(it, null).toOptional() },
                next = { loadBalance(safe, it).map { it.toOptional() } }
            )
            .startWith(None)

    private fun loadBalance(address: Solidity.Address, token: ERC20Token) =
        tokenRepository.loadTokenBalances(address, listOf(token))
            .map {
                val (erc20Token, balance) = it.first()
                ERC20TokenWithBalance(erc20Token, balance)
            }
            .onErrorReturn { ERC20TokenWithBalance(token, null) }

    private fun setupTokenCheck(
        safe: Solidity.Address,
        token: ERC20TokenWithBalance,
        receipient: Solidity.Address,
        value: BigDecimal,
        reviewEvents: Observable<Unit>
    ): Observable<Result<ViewUpdate>> {
        val amount = value.multiply(BigDecimal(10).pow(token.token.decimals)).toBigInteger()
        // Not enough funds
        if (token.balance == null || token.balance < amount) return Observable.just(DataResult(ViewUpdate.InvalidInput(true, false)))
        val data = TransactionData.AssetTransfer(token.token.address, amount, receipient)
        return Observable.merge(
            estimate(safe, data),
            reviewEvents
                .subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    val intent = ReviewTransactionActivity.createIntent(context, safe, data)
                    ViewUpdate.StartReview(intent)
                }
                .mapToResult()
        )
    }

    private fun estimate(safe: Solidity.Address, data: TransactionData.AssetTransfer) =
        Observable.fromCallable {
            AssetTransferTransactionBuilder.build(data)
        }.flatMapSingle {
            executionRepository.loadExecuteInformation(safe, it)
        }
            .map<ViewUpdate> {
                val estimate = Wei(it.estimate.multiply(it.gasPrice))
                val canExecute =
                    (estimate.value + (if (data.token == ERC20Token.ETHER_TOKEN.address) data.amount else BigInteger.ZERO)) <= it.balance.value
                ViewUpdate.Estimate(estimate, it.balance, canExecute)
            }
            .onErrorReturn { ViewUpdate.EstimateError }
            .mapToResult()
}

abstract class CreateAssetTransferContract : ViewModel() {
    abstract fun processInput(
        safe: Solidity.Address,
        tokenAddress: Solidity.Address,
        reviewEvents: Observable<Unit>
    ): ObservableTransformer<Input, Result<ViewUpdate>>

    data class Input(val amount: String, val address: String)

    sealed class ViewUpdate {
        data class Estimate(val estimate: Wei, val balance: Wei, val canExecute: Boolean) : ViewUpdate()
        object EstimateError : ViewUpdate()
        data class TokenInfo(val value: ERC20TokenWithBalance) : ViewUpdate()
        data class InvalidInput(val amount: Boolean, val address: Boolean) : ViewUpdate()
        data class StartReview(val intent: Intent) : ViewUpdate()
    }
}

class CreateAssetTransferActivity : ViewModelActivity<CreateAssetTransferContract>() {

    @Inject
    lateinit var addressHelper: AddressHelper

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
            layout_create_asset_transfer_input_receiver.setTextColor(ContextCompat.getColor(this, R.color.dark_slate_blue))
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
                            layout_create_asset_transfer_input_value.setTextColor(ContextCompat.getColor(this, R.color.dark_slate_blue))
                        },
                    layout_create_asset_transfer_input_receiver.textChanges()
                        .doOnNext {
                            layout_create_asset_transfer_input_receiver.setTextColor(ContextCompat.getColor(this, R.color.dark_slate_blue))
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
    }

    private fun applyUpdate(update: ViewUpdate) {
        when (update) {
            is CreateAssetTransferContract.ViewUpdate.Estimate -> {
                layout_create_asset_transfer_balance_value.text =
                        getString(R.string.x_ether, update.balance.toEther().stringWithNoTrailingZeroes())
                layout_create_asset_transfer_fees_value.text =
                        "- ${getString(R.string.x_ether, update.estimate.toEther().stringWithNoTrailingZeroes())}"
                layout_create_asset_transfer_continue_button.visible(update.canExecute)
                if (!update.canExecute) layout_create_asset_transfer_input_value.setTextColor(ContextCompat.getColor(this, R.color.tomato))
                else {
                    layout_create_asset_transfer_input_receiver.setTextColor(ContextCompat.getColor(this, R.color.dark_slate_blue))
                    layout_create_asset_transfer_input_value.setTextColor(ContextCompat.getColor(this, R.color.dark_slate_blue))
                }
            }
            CreateAssetTransferContract.ViewUpdate.EstimateError -> disableContinue()
            is CreateAssetTransferContract.ViewUpdate.TokenInfo -> {
                update.value.token.name?.let{
                    layout_create_asset_transfer_title.text = getString(R.string.transfer_x, it)
                }
                layout_create_asset_transfer_safe_balance.text = update.value.displayString()
                layout_create_asset_transfer_input_label.text = update.value.token.symbol ?: "???"
            }
            is CreateAssetTransferContract.ViewUpdate.InvalidInput -> {
                layout_create_asset_transfer_input_value.setTextColor(
                    ContextCompat.getColor(
                        this,
                        if (update.amount) R.color.tomato else R.color.dark_slate_blue
                    )
                )
                layout_create_asset_transfer_input_receiver.setTextColor(
                    ContextCompat.getColor(
                        this,
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
