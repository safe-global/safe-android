package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_create_add_recovery_extension.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
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
import pm.gnosis.utils.hexAsBigIntegerOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class CreateAddRecoveryExtensionFragment : BaseEditableTransactionDetailsFragment() {
    @Inject
    lateinit var subViewModel: AddRecoveryExtensionContract

    private var transaction: Transaction? = null
    private var safe: Solidity.Address? = null
    private var addressForAccount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safe = arguments?.getString(ARG_SAFE)?.asEthereumAddress()
        transaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_create_add_recovery_extension, container, false)

    override fun onStart() {
        super.onStart()

        disposables += observeSafe()
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { it.toNullable()?.let { Observable.just(it) } ?: Observable.empty() }
            .compose(updateSafeInfoTransformer(layout_view_add_recovery_extension_safe_address))
            .subscribeBy(onError = Timber::e)
    }

    override fun observeTransaction(): Observable<Result<SafeTransaction>> =
    // Setup initial form data
        subViewModel.loadRecoveryOwners(transaction)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { (address1, address2) ->
                layout_view_add_recovery_extension_acc_1_address_input.setDefault(address1?.asEthereumAddressString())
                layout_view_add_recovery_extension_acc_1_address_book_button.setOnClickListener {
                    addressForAccount = 1
                    QRCodeScanActivity.startForResult(this)
                }
                layout_view_add_recovery_extension_acc_1_scan_address_button.setOnClickListener {
                    addressForAccount = 1
                    selectFromAddressBook()
                }
                layout_view_add_recovery_extension_acc_2_address_input.setDefault(address2?.asEthereumAddressString())
                layout_view_add_recovery_extension_acc_2_address_book_button.setOnClickListener {
                    addressForAccount = 2
                    QRCodeScanActivity.startForResult(this)
                }
                layout_view_add_recovery_extension_acc_2_scan_address_button.setOnClickListener {
                    addressForAccount = 2
                    selectFromAddressBook()
                }
            }
            // Setup input
            .flatMapObservable {
                Observable.combineLatest(
                    prepareInput(layout_view_add_recovery_extension_acc_1_address_input),
                    prepareInput(layout_view_add_recovery_extension_acc_2_address_input),
                    BiFunction<CharSequence, CharSequence, Pair<CharSequence, CharSequence>> { t1, t2 -> t1 to t2 }
                )

            }
            .compose(subViewModel.inputTransformer(safe))
            // Update for errors
            .doOnNextForResult(onError = {
                (it as? TransactionInputException)?.let {
                    if (it.errorFields and TransactionInputException.TARGET_FIELD != 0) {
                        setInputError(layout_view_add_recovery_extension_acc_1_address_input)
                        setInputError(layout_view_add_recovery_extension_acc_2_address_input)
                    }
                }
            })

    override fun onAddressProvided(address: Solidity.Address) {
        when (addressForAccount) {
            1 -> layout_view_add_recovery_extension_acc_1_address_input.setText(address.asEthereumAddressString())
            2 -> layout_view_add_recovery_extension_acc_2_address_input.setText(address.asEthereumAddressString())
        }

    }

    override fun observeSafe(): Observable<Optional<Solidity.Address>> =
        Observable.just(safe.toOptional())

    override fun inputEnabled(enabled: Boolean) {
        layout_view_add_recovery_extension_acc_1_address_input.isEnabled = enabled
        layout_view_add_recovery_extension_acc_1_scan_address_button.isEnabled = enabled
        layout_view_add_recovery_extension_acc_1_address_book_button.isEnabled = enabled
        layout_view_add_recovery_extension_acc_2_address_input.isEnabled = enabled
        layout_view_add_recovery_extension_acc_2_scan_address_button.isEnabled = enabled
        layout_view_add_recovery_extension_acc_2_address_book_button.isEnabled = enabled
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
            CreateAddRecoveryExtensionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TRANSACTION, transaction?.parcelable())
                    putString(ARG_SAFE, safeAddress)
                }
            }
    }
}
