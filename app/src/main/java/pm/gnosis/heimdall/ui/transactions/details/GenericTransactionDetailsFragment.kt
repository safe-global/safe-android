package pm.gnosis.heimdall.ui.transactions.details

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_transaction_details_generic.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit


class GenericTransactionDetailsFragment : BaseTransactionDetailsFragment() {

    private val safeSubject = BehaviorSubject.createDefault<Optional<BigInteger>>(None)
    private val inputSubject = PublishSubject.create<CombinedRawInput>()
    private var editable: Boolean = false
    private var originalTransaction: Transaction? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.layout_transaction_details_generic, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editable = arguments?.getBoolean(ARG_EDITABLE, false) ?: false
        val transaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
        originalTransaction = transaction
        val safe = arguments?.getString(ARG_SAFE)?.hexAsBigIntegerOrNull()
        layout_transaction_details_generic_to_input.setText(transaction?.address?.asEthereumAddressStringOrNull())
        layout_transaction_details_generic_data_input.setText(transaction?.data)
        layout_transaction_details_generic_value_input.setText(transaction?.value?.value?.asDecimalString())
        toggleTransactionInput(editable)
        setupSafeSpinner(layout_transaction_details_generic_safe_input, safe)

        disposables += Observable.combineLatest(
                prepareInput(layout_transaction_details_generic_to_input),
                prepareInput(layout_transaction_details_generic_value_input),
                prepareInput(layout_transaction_details_generic_data_input),
                Function3 { to: CharSequence, value: CharSequence, data: CharSequence ->
                    CombinedRawInput(to.toString() to false, value.toString() to false, data.toString() to false)
                }
        ).subscribe(inputSubject::onNext, Timber::e)
    }

    override fun inputEnabled(enabled: Boolean) {
        if (editable) {
            toggleTransactionInput(enabled)
        }
    }

    private fun toggleTransactionInput(enabled: Boolean) {
        layout_transaction_details_generic_safe_input.isEnabled = enabled
        layout_transaction_details_generic_to_input.isEnabled = enabled
        layout_transaction_details_generic_data_input.isEnabled = enabled
        layout_transaction_details_generic_value_input.isEnabled = enabled
    }

    override fun selectedSafeChanged(safe: Safe?) {
        safeSubject.onNext(safe?.address?.toOptional() ?: None)
    }

    override fun observeTransaction(): Observable<Result<Transaction>> {
        return inputSubject
                .scan { old, new -> old.diff(new) }
                .map {
                    val to = it.to.first.hexAsEthereumAddressOrNull()
                    val data = it.data.first.hexStringToByteArrayOrNull()
                    val value = it.value.first.decimalAsBigIntegerOrNull()
                    var errorFields = 0
                    var showToast = false
                    if (to == null) {
                        errorFields = errorFields or TransactionInputException.TO_FIELD
                        showToast = showToast or it.to.second
                    }
                    if (it.data.first.isNotBlank() && data == null) {
                        errorFields = errorFields or TransactionInputException.DATA_FIELD
                        showToast = showToast or it.data.second
                    }
                    if (value == null) {
                        errorFields = errorFields or TransactionInputException.VALUE_FIELD
                        showToast = showToast or it.value.second
                    }
                    if (errorFields > 0) {
                        ErrorResult<Transaction>(TransactionInputException(context!!, errorFields, showToast))
                    } else {
                        val nonce = originalTransaction?.nonce ?: BigInteger.valueOf(System.currentTimeMillis())
                        DataResult(Transaction(to!!, value = Wei(value!!), data = data?.toHexString(), nonce = nonce))
                    }
                }
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

    private fun setInputError(input: TextView) {
        input.setTextColor(ContextCompat.getColor(context!!, R.color.error))
        input.setHintTextColor(ContextCompat.getColor(context!!, R.color.error_hint))
    }

    private fun prepareInput(input: TextView): Observable<CharSequence> =
            input
                    .textChanges()
                    .debounce(INPUT_DELAY_MS, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        input.setTextColor(ContextCompat.getColor(context!!, R.color.gnosis_dark_blue))
                        input.setHintTextColor(ContextCompat.getColor(context!!, R.color.gnosis_dark_blue_alpha_70))
                    }

    override fun observeSafe(): Observable<Optional<BigInteger>> {
        return safeSubject
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(activity!!))
                .build().inject(this)
    }

    // Field and if the field has changed
    private data class CombinedRawInput(val to: Pair<String, Boolean>, val value: Pair<String, Boolean>, val data: Pair<String, Boolean>) {
        fun diff(other: CombinedRawInput): CombinedRawInput =
                CombinedRawInput(check(this.to, other.to), check(this.value, other.value), check(this.data, other.data))

        companion object {
            private fun check(current: Pair<String, Boolean>, change: Pair<String, Boolean>): Pair<String, Boolean> =
                    change.first to (current.first != change.first)
        }
    }

    class TransactionInputException(context: Context, val errorFields: Int, val showSnackbar: Boolean) : LocalizedException(
            context.getString(R.string.error_transaction_params)
    ) {
        companion object {
            const val TO_FIELD = 1
            const val VALUE_FIELD = 1 shl 1
            const val DATA_FIELD = 1 shl 2
        }
    }

    companion object {

        private const val INPUT_DELAY_MS = 500L
        private const val ARG_TRANSACTION = "argument.parcelable.transaction"
        private const val ARG_SAFE = "argument.string.safe"
        private const val ARG_EDITABLE = "argument.boolean.editable"

        fun createInstance(transaction: Transaction?, safeAddress: String?, editable: Boolean) =
                GenericTransactionDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean(ARG_EDITABLE, editable)
                        putParcelable(ARG_TRANSACTION, transaction?.parcelable())
                        putString(ARG_SAFE, safeAddress)
                    }
                }
    }
}