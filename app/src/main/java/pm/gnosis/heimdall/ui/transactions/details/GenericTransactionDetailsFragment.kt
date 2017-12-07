package pm.gnosis.heimdall.ui.transactions.details

import android.content.Context
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
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_transaction_details_generic.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.asEthereumAddressStringOrNull
import pm.gnosis.utils.hexAsBigIntegerOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


class GenericTransactionDetailsFragment : BaseTransactionDetailsFragment() {

    @Inject
    lateinit var subViewModel: GenericTransactionDetailsContract

    private val safeSubject = BehaviorSubject.createDefault<Optional<BigInteger>>(None)
    private val inputSubject = PublishSubject.create<GenericTransactionDetailsContract.CombinedRawInput>()
    private var editable: Boolean = false
    private var originalTransaction: Transaction? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.layout_transaction_details_generic, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editable = arguments?.getBoolean(ARG_EDITABLE, false) ?: false
        val transaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
        originalTransaction = transaction
        layout_transaction_details_generic_to_input.setText(transaction?.address?.asEthereumAddressStringOrNull())
        layout_transaction_details_generic_data_input.setText(transaction?.data)
        layout_transaction_details_generic_value_input.setText(transaction?.value?.value?.asDecimalString())
        toggleTransactionInput(editable)
    }

    override fun onStart() {
        super.onStart()
        val safe = arguments?.getString(ARG_SAFE)?.hexAsBigIntegerOrNull()
        setupSafeSpinner(layout_transaction_details_generic_safe_input, safe)
        disposables += Observable.combineLatest(
                prepareInput(layout_transaction_details_generic_to_input),
                prepareInput(layout_transaction_details_generic_value_input),
                prepareInput(layout_transaction_details_generic_data_input),
                Function3 { to: CharSequence, value: CharSequence, data: CharSequence ->
                    GenericTransactionDetailsContract.CombinedRawInput(to.toString() to false, value.toString() to false, data.toString() to false)
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

    class TransactionInputException(context: Context, val errorFields: Int, val showSnackbar: Boolean) : LocalizedException(
            context.getString(R.string.error_transaction_params)
    ) {
        companion object {
            const val TO_FIELD = 1
            const val VALUE_FIELD = 1 shl 1
            const val DATA_FIELD = 1 shl 2
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as TransactionInputException

            if (errorFields != other.errorFields) return false
            if (showSnackbar != other.showSnackbar) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + errorFields
            result = 31 * result + showSnackbar.hashCode()
            return result
        }
    }

    companion object {

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