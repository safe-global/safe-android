package pm.gnosis.heimdall.ui.transactions.details.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_create_add_safe_owner.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.doOnNextForResult
import pm.gnosis.heimdall.ui.transactions.details.base.BaseEditableTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.hexAsBigIntegerOrNull
import java.math.BigInteger
import javax.inject.Inject


class CreateAddOwnerDetailsFragment : BaseEditableTransactionDetailsFragment() {
    @Inject
    lateinit var subViewModel: ChangeSafeSettingsDetailsContract

    private var transaction: Transaction? = null
    private var safe: BigInteger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safe = arguments?.getString(ARG_SAFE)?.hexAsBigIntegerOrNull()
        transaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.layout_create_add_safe_owner, container, false)

    override fun observeTransaction(): Observable<Result<Transaction>> =
            // Setup initial form data
            subViewModel.loadFormData(transaction)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { (address) ->
                        layout_create_add_safe_owner_address_input.setText(address)
                        layout_create_add_safe_owner_address_input.setSelection(address.length)
                    }
                    // Setup input
                    .flatMapObservable {
                        prepareInput(layout_create_add_safe_owner_address_input)
                    }
                    .compose(subViewModel.inputTransformer(safe))
                    // Update for errors
                    .doOnNextForResult(onError = {
                        (it as? TransactionInputException)?.let {
                            if (it.errorFields and TransactionInputException.TARGET_FIELD != 0) {
                                setInputError(layout_create_add_safe_owner_address_input)
                            }
                        }
                    })

    override fun observeSafe(): Observable<Optional<BigInteger>> =
            Observable.just(safe.toOptional())

    override fun inputEnabled(enabled: Boolean) {
        layout_create_add_safe_owner_address_input.isEnabled = enabled
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
                CreateAddOwnerDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_TRANSACTION, transaction?.parcelable())
                        putString(ARG_SAFE, safeAddress)
                    }
                }
    }

}