package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import android.os.Bundle
import android.support.annotation.LayoutRes
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
import kotlinx.android.synthetic.main.layout_receipt_add_recovery_extension.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.addressbook.helpers.AddressInfoViewHolder
import pm.gnosis.heimdall.ui.base.InflatedViewProvider
import pm.gnosis.heimdall.ui.transactions.details.base.BaseReviewTransactionDetailsFragment
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigIntegerOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

abstract class ViewAddRecoveryExtensionDetailsFragment : BaseReviewTransactionDetailsFragment() {

    private val firstRecoveryAccViewProvider by lazy { InflatedViewProvider(layout_view_add_recovery_extension_recovery_owner_1_container) }
    private val secondRecoveryAccViewProvider by lazy { InflatedViewProvider(layout_view_add_recovery_extension_recovery_owner_2_container) }

    @Inject
    lateinit var subViewModel: AddRecoveryExtensionContract

    private var transaction: SafeTransaction? = null
    private var safe: Solidity.Address? = null

    @LayoutRes
    abstract fun layout(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safe = arguments?.getString(ARG_SAFE)?.asEthereumAddress()
        transaction = arguments?.getParcelable(ARG_TRANSACTION)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(layout(), container, false)

    override fun onStart() {
        super.onStart()

        disposables += subViewModel.loadRecoveryOwners(transaction?.wrapped)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::setupForm, Timber::e)

        safe?.let {
            disposables += Observable.just(it)
                .compose(updateSafeInfoTransformer(layout_view_add_recovery_extension_address))
                .subscribeBy(onError = Timber::e)
        }
    }

    private fun setupForm(recoverOwners: Pair<Solidity.Address?, Solidity.Address?>) {
        AddressInfoViewHolder(this, firstRecoveryAccViewProvider).apply {
            bind(recoverOwners.first)
            view.layout_address_item_label.setText(R.string.recovery_owner_1)
        }

        AddressInfoViewHolder(this, secondRecoveryAccViewProvider).apply {
            bind(recoverOwners.second)
            view.layout_address_item_label.setText(R.string.recovery_owner_2)
        }
    }

    override fun observeTransaction(): Observable<Result<SafeTransaction>> {
        return Observable.just(transaction.toOptional())
            .flatMap { it.toNullable()?.let { Observable.just(it) ?: Observable.empty() } }
            .mapToResult()
    }

    override fun observeSafe(): Observable<Optional<Solidity.Address>> =
        Observable.just(safe.toOptional())

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(activity!!))
            .build().inject(this)
    }

    companion object {

        private const val ARG_TRANSACTION = "argument.parcelable.wrapped"
        private const val ARG_SAFE = "argument.string.safe"

        fun createBundle(transaction: SafeTransaction?, safeAddress: String?) =
            Bundle().apply {
                putParcelable(ARG_TRANSACTION, transaction)
                putString(ARG_SAFE, safeAddress)
            }
    }
}
