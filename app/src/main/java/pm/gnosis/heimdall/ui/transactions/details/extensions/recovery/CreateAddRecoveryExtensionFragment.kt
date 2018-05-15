package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.include_address_item_with_label_start_and_icon.view.*
import kotlinx.android.synthetic.main.layout_create_add_recovery_extension.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.addressbook.helpers.AddressInfoViewHolder
import pm.gnosis.heimdall.ui.base.InflatedViewProvider
import pm.gnosis.heimdall.ui.transactions.details.base.BaseEditableTransactionDetailsFragment
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.utils.asEthereumAddress
import timber.log.Timber
import javax.inject.Inject

class CreateAddRecoveryExtensionFragment : BaseEditableTransactionDetailsFragment() {

    private val recoveryAccViewProvider by lazy { InflatedViewProvider(layout_create_add_recovery_extension_recovery_owner_container) }

    @Inject
    lateinit var subViewModel: AddRecoveryExtensionContract

    private var transaction: Transaction? = null
    private var safe: Solidity.Address? = null

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
            .compose(updateSafeInfoTransformer(layout_create_add_recovery_extension_safe_address))
            .subscribeBy(onError = Timber::e)
    }

    override fun observeTransaction(): Observable<Result<SafeTransaction>> =
    // Setup initial form data
        subViewModel.loadCreateRecoveryInfo()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { (recoverer, mnemonic) ->
                AddressInfoViewHolder(this, recoveryAccViewProvider).apply {
                    bind(recoverer)
                    view.layout_address_item_label.setText(R.string.recovery_id)
                }
                layout_create_add_recovery_extension_recovery_mnemonic.text = mnemonic
            }
            .map { it.first }
            .toObservable()
            .compose(subViewModel.inputTransformer(safe))

    override fun observeSafe(): Observable<Optional<Solidity.Address>> =
        Observable.just(safe.toOptional())

    override fun inputEnabled(enabled: Boolean) {
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
