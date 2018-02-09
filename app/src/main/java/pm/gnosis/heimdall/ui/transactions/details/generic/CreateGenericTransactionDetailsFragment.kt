package pm.gnosis.heimdall.ui.transactions.details.generic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_create_transaction_safe_info.*
import kotlinx.android.synthetic.main.layout_transaction_details_generic.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.scanQrCode
import pm.gnosis.heimdall.common.utils.visible
import pm.gnosis.heimdall.ui.transactions.details.base.BaseEditableTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.asEthereumAddressStringOrNull
import pm.gnosis.utils.hexAsBigIntegerOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


class CreateGenericTransactionDetailsFragment : BaseEditableTransactionDetailsFragment() {

    @Inject
    lateinit var subViewModel: GenericTransactionDetailsContract

    private val safeSubject = BehaviorSubject.createDefault<Optional<BigInteger>>(None)
    private val inputSubject = PublishSubject.create<GenericTransactionDetailsContract.InputEvent>()
    private var editable: Boolean = false
    private var originalTransaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safeSubject.onNext(arguments?.getString(ARG_SAFE)?.hexAsBigIntegerOrNull().toOptional())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.layout_transaction_details_generic, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editable = arguments?.getBoolean(ARG_EDITABLE, false) ?: false
        val transaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
        originalTransaction = transaction
        layout_transaction_details_generic_to_input.setDefault(transaction?.address?.asEthereumAddressStringOrNull())
        layout_transaction_details_generic_data_input.setDefault(transaction?.data)
        // If it is editable we leave the field empty if no value is present
        val value = (transaction?.value?.value ?: if (editable) null else BigInteger.ZERO)
        layout_transaction_details_generic_value_input.setDefault(value?.asDecimalString())
        layout_transaction_details_generic_scan_to_button.visible(editable)
        layout_transaction_details_generic_divider_qr_code.visible(editable)
        toggleTransactionInput(editable)
    }

    override fun onStart() {
        super.onStart()
        disposables += observeSafe()
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { it.toNullable()?.let { Observable.just(it) } ?: Observable.empty() }
                .compose(updateSafeInfoTransformer(layout_create_transaction_safe_info_address))
                .subscribeBy(onError = Timber::e)

        disposables += Observable.combineLatest(
                prepareInput(layout_transaction_details_generic_to_input),
                prepareInput(layout_transaction_details_generic_value_input),
                prepareInput(layout_transaction_details_generic_data_input),
                Function3 { to: CharSequence, value: CharSequence, data: CharSequence ->
                    GenericTransactionDetailsContract.InputEvent(to.toString() to false, value.toString() to false, data.toString() to false)
                }
        ).subscribe(inputSubject::onNext, Timber::e)

        layout_transaction_details_generic_scan_to_button.setOnClickListener {
            scanQrCode()
        }
    }

    override fun inputEnabled(enabled: Boolean) {
        if (editable) {
            toggleTransactionInput(enabled)
        }
    }

    private fun toggleTransactionInput(enabled: Boolean) {
        layout_transaction_details_generic_to_input.isEnabled = enabled
        layout_transaction_details_generic_data_input.isEnabled = enabled
        layout_transaction_details_generic_value_input.isEnabled = enabled
        layout_transaction_details_generic_scan_to_button.isEnabled = enabled
    }

    override fun observeTransaction(): Observable<Result<Transaction>> {
        return inputSubject
                .compose(subViewModel.inputTransformer(context!!, originalTransaction))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    (it as? ErrorResult)?.let {
                        (it.error as? TransactionInputException)?.let {
                            if (it.errorFields and TransactionInputException.TO_FIELD != 0) {
                                setInputError(layout_transaction_details_generic_to_input)
                            }
                            if (it.errorFields and TransactionInputException.DATA_FIELD != 0) {
                                setInputError(layout_transaction_details_generic_data_input)
                            }
                            if (it.errorFields and TransactionInputException.VALUE_FIELD != 0) {
                                setInputError(layout_transaction_details_generic_value_input)
                            }
                        }
                    }
                }
    }

    override fun observeSafe(): Observable<Optional<BigInteger>> = safeSubject

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(activity!!))
                .build().inject(this)
    }

    companion object {

        private const val ARG_TRANSACTION = "argument.parcelable.transaction"
        private const val ARG_SAFE = "argument.string.safe"
        private const val ARG_EDITABLE = "argument.boolean.editable"

        fun createInstance(transaction: Transaction?, safeAddress: String?, editable: Boolean) =
                CreateGenericTransactionDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean(ARG_EDITABLE, editable)
                        putParcelable(ARG_TRANSACTION, transaction?.parcelable())
                        putString(ARG_SAFE, safeAddress)
                    }
                }
    }
}