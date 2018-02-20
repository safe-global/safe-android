package pm.gnosis.heimdall.ui.transactions.details.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_create_transaction_safe_info.*
import kotlinx.android.synthetic.main.layout_transaction_details_asset_transfer.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.transactions.details.base.BaseEditableTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.heimdall.utils.selectFromAddressBook
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.asEthereumAddressStringOrNull
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.stringWithNoTrailingZeroes
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


class CreateAssetTransferDetailsFragment : BaseEditableTransactionDetailsFragment() {

    @Inject
    lateinit var subViewModel: AssetTransferDetailsContract

    private val safeSubject = BehaviorSubject.createDefault<Optional<BigInteger>>(None)
    private val inputSubject = PublishSubject.create<AssetTransferDetailsContract.InputEvent>()
    private var cachedTransaction: Transaction? = null

    private fun updateTokenInfoTransformer(token: ERC20Token) = ObservableTransformer<BigInteger, Result<ERC20TokenWithBalance>> {
        it.switchMap { subViewModel.loadTokenInfo(it, token) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNextForResult({
                    layout_create_transaction_safe_info_balance.text = it.displayString()
                    layout_transaction_details_asset_transfer_max_amount_button.visible(true)
                })
                .switchMap { info ->
                    layout_transaction_details_asset_transfer_max_amount_button.clicks().map { info }
                }
                .doOnNextForResult({ info ->
                    info.balance?.let { layout_transaction_details_asset_transfer_amount_input.setText(info.token.convertAmount(it).stringWithNoTrailingZeroes()) }
                            ?: run {
                                snackbar(layout_transaction_details_asset_transfer_amount_input, getString(R.string.error_no_token_info))
                            }
                })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cachedTransaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
        safeSubject.onNext(arguments?.getString(ARG_SAFE)?.hexAsBigIntegerOrNull().toOptional())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.layout_transaction_details_asset_transfer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layout_transaction_details_asset_transfer_max_amount_button.visibility = View.VISIBLE
        layout_transaction_details_asset_transfer_amount_input_container.setOnClickListener {
            layout_transaction_details_asset_transfer_amount_input.showKeyboardForView()
        }
    }

    override fun onStart() {
        super.onStart()

        disposables += subViewModel.loadFormData(cachedTransaction, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::setupForm, Timber::e)
    }

    private fun setupForm(info: AssetTransferDetailsContract.FormData) {
        layout_transaction_details_asset_transfer_to_input.setDefault(info.to?.asEthereumAddressStringOrNull())
        layout_transaction_details_asset_transfer_amount_input.setDefault(info.tokenAmount?.let { info.token?.convertAmount(it)?.stringWithNoTrailingZeroes() })
        layout_transaction_details_asset_transfer_amount_input.setCurrencySymbol(info.token?.symbol)
        // Load info
        info.token?.let { token ->
            disposables += observeSafe()
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { it.toNullable()?.let { Observable.just(it) } ?: Observable.empty() }
                    .publish {
                        Observable.merge(
                                it.compose(updateSafeInfoTransformer(layout_create_transaction_safe_info_address)),
                                it.compose(updateTokenInfoTransformer(token))
                        )
                    }
                    .subscribeBy(onError = Timber::e)
        }

        // Setup input
        disposables += Observable.combineLatest(
                prepareInput(layout_transaction_details_asset_transfer_to_input),
                prepareInput(layout_transaction_details_asset_transfer_amount_input),
                BiFunction { to: CharSequence, amount: CharSequence ->
                    AssetTransferDetailsContract.InputEvent(to.toString() to false, amount.toString() to false, info.token to false)
                }
        ).subscribe(inputSubject::onNext, Timber::e)

        layout_transaction_details_asset_transfer_scan_to_button.setOnClickListener {
            scanQrCode()
        }

        layout_transaction_details_asset_transfer_address_book_button.setOnClickListener {
            selectFromAddressBook()
        }
    }

    override fun onAddressProvided(address: BigInteger) {
        layout_transaction_details_asset_transfer_to_input.setText(address.asEthereumAddressString())
    }

    override fun observeTransaction(): Observable<Result<Transaction>> {
        return inputSubject
                .compose(subViewModel.inputTransformer(cachedTransaction))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNextForResult({
                    cachedTransaction = it.copy(nonce = cachedTransaction?.nonce ?: it.nonce)
                }, {
                    (it as? TransactionInputException)?.let {
                        if (it.errorFields and TransactionInputException.TO_FIELD != 0) {
                            setInputError(layout_transaction_details_asset_transfer_to_input)
                        }
                        if (it.errorFields and TransactionInputException.AMOUNT_FIELD != 0) {
                            setInputError(layout_transaction_details_asset_transfer_amount_input)
                        }
                    }
                })
    }

    override fun observeSafe(): Observable<Optional<BigInteger>> = safeSubject

    override fun inputEnabled(enabled: Boolean) {
        layout_transaction_details_asset_transfer_to_input.isEnabled = enabled
        layout_transaction_details_asset_transfer_amount_input.isEnabled = enabled
        layout_transaction_details_asset_transfer_max_amount_button.isEnabled = enabled
        layout_transaction_details_asset_transfer_scan_to_button.isEnabled = enabled
        layout_transaction_details_asset_transfer_address_book_button.isEnabled = enabled
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(activity!!))
                .build().inject(this)
    }

    companion object {

        private const val ARG_TRANSACTION = "argument.parcelable.transaction"
        private const val ARG_SAFE = "argument.string.safe"

        fun createInstance(transaction: Transaction?, safeAddress: String?) =
                CreateAssetTransferDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_TRANSACTION, transaction?.parcelable())
                        putString(ARG_SAFE, safeAddress)
                    }
                }
    }
}