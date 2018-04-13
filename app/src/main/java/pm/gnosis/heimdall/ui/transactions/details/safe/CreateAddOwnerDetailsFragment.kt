package pm.gnosis.heimdall.ui.transactions.details.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_create_add_safe_owner.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.transactions.details.base.BaseEditableTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.heimdall.utils.selectFromAddressBook
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.doOnNextForResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class CreateAddOwnerDetailsFragment : BaseEditableTransactionDetailsFragment() {
    @Inject
    lateinit var subViewModel: ChangeSafeSettingsDetailsContract

    private var transaction: Transaction? = null
    private var safe: Solidity.Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safe = arguments?.getString(ARG_SAFE)?.asEthereumAddress()
        transaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_create_add_safe_owner, container, false)

    override fun onStart() {
        super.onStart()

        disposables += observeSafe()
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { it.toNullable()?.let { Observable.just(it) } ?: Observable.empty() }
            .compose(updateSafeInfoTransformer(layout_create_add_safe_owner_safe_address))
            .subscribeBy(onError = Timber::e)

        disposables += layout_create_add_safe_owner_address_book_button.clicks()
            .subscribeBy(onNext = {
                selectFromAddressBook()
            }, onError = Timber::e)
    }

    override fun observeTransaction(): Observable<Result<SafeTransaction>> =
    // Setup initial form data
        subViewModel.loadFormData(transaction)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { (address) ->
                layout_create_add_safe_owner_address_input.setDefault(address)
                layout_create_add_safe_owner_scan_address_button.setOnClickListener {
                    QRCodeScanActivity.startForResult(this)
                }
                layout_create_add_safe_owner_scan_address_button.setOnClickListener {
                    selectFromAddressBook()
                }
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

    override fun onAddressProvided(address: Solidity.Address) {
        layout_create_add_safe_owner_address_input.setText(address.asEthereumAddressString())
    }

    override fun observeSafe(): Observable<Optional<Solidity.Address>> = Observable.just(safe.toOptional())

    override fun inputEnabled(enabled: Boolean) {
        layout_create_add_safe_owner_address_input.isEnabled = enabled
        layout_create_add_safe_owner_scan_address_button.isEnabled = enabled
        layout_create_add_safe_owner_address_book_button.isEnabled = enabled
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(activity!!))
            .build().inject(this)
    }

    companion object {

        private const val ARG_TRANSACTION = "argument.parcelable.wrapped"
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
