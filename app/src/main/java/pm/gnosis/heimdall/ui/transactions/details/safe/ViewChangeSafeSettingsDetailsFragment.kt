package pm.gnosis.heimdall.ui.transactions.details.safe

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
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
import kotlinx.android.synthetic.main.layout_receipt_change_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.common.utils.visible
import pm.gnosis.heimdall.ui.addressbook.helpers.AddressInfoViewHolder
import pm.gnosis.heimdall.ui.addressbook.helpers.InflatedViewFactory
import pm.gnosis.heimdall.ui.transactions.details.base.BaseReviewTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.safe.ChangeSafeSettingsDetailsContract.Action.*
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asEthereumAddressStringOrNull
import pm.gnosis.utils.hexAsBigIntegerOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


abstract class ViewChangeSafeSettingsDetailsFragment : BaseReviewTransactionDetailsFragment() {

    private val primaryTargetViewFactory by lazy { InflatedViewFactory(layout_view_change_safe_primary_target_container) }
    private val secondaryTargetViewFactory by lazy { InflatedViewFactory(layout_view_change_safe_secondary_target_container) }

    @Inject
    lateinit var subViewModel: ChangeSafeSettingsDetailsContract

    private var transaction: Transaction? = null
    private var safe: BigInteger? = null

    @LayoutRes
    abstract fun layout(): Int

    @StringRes
    abstract fun removedOwnerMessage(): Int

    @StringRes
    abstract fun addedOwnerMessage(): Int

    @StringRes
    abstract fun replacedOwnerMessage(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safe = arguments?.getString(ARG_SAFE)?.hexAsBigIntegerOrNull()
        transaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(layout(), container, false)

    override fun onStart() {
        super.onStart()

        disposables += subViewModel.loadAction(safe, transaction)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::setupForm, Timber::e)

        safe?.let {
            disposables += Observable.just(it)
                    .compose(updateSafeInfoTransformer(layout_view_change_safe_address))
                    .subscribeBy(onError = Timber::e)
        }
    }

    private fun setupForm(action: ChangeSafeSettingsDetailsContract.Action) {
        when (action) {
            is RemoveOwner -> {
                AddressInfoViewHolder(this, primaryTargetViewFactory).apply {
                    bind(action.owner)
                    view.layout_address_item_label.setText(R.string.old_owner)
                }
                layout_view_change_safe_secondary_target_container.visible(false)
                layout_view_change_safe_action.setText(removedOwnerMessage())
            }
            is AddOwner -> {
                AddressInfoViewHolder(this, primaryTargetViewFactory).apply {
                    bind(action.owner)
                    view.layout_address_item_label.setText(R.string.new_owner)
                }
                layout_view_change_safe_secondary_target_container.visible(false)
                layout_view_change_safe_action.setText(addedOwnerMessage())
            }
            is ReplaceOwner -> {
                AddressInfoViewHolder(this, primaryTargetViewFactory).apply {
                    bind(action.newOwner)
                    view.layout_address_item_label.setText(R.string.new_owner)
                }

                layout_view_change_safe_secondary_target_container.visible(true)
                AddressInfoViewHolder(this, secondaryTargetViewFactory).apply {
                    bind(action.previousOwner)
                    view.layout_address_item_label.setText(R.string.new_owner)
                }
                layout_view_change_safe_action.setText(replacedOwnerMessage())
            }
        }
    }

    override fun observeTransaction(): Observable<Result<Transaction>> {
        return Observable.just(transaction.toOptional())
                .flatMap { it.toNullable()?.let { Observable.just(it) ?: Observable.empty() } }
                .mapToResult()
    }

    override fun observeSafe(): Observable<Optional<BigInteger>> =
            Observable.just(safe.toOptional())

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(activity!!))
                .build().inject(this)
    }

    companion object {

        private const val ARG_TRANSACTION = "argument.parcelable.transaction"
        private const val ARG_SAFE = "argument.string.safe"

        fun createBundle(transaction: Transaction?, safeAddress: String?) =
                Bundle().apply {
                    putParcelable(ARG_TRANSACTION, transaction?.parcelable())
                    putString(ARG_SAFE, safeAddress)
                }
    }

}
