package pm.gnosis.heimdall.ui.transactions.details.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_review_change_safe_owner.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.ui.transactions.details.base.BaseReviewTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.safe.ChangeSafeSettingsDetailsContract.Action.*
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.hexAsBigIntegerOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


class ReviewChangeDeviceSettingsDetailsFragment : BaseReviewTransactionDetailsFragment() {
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
            = inflater.inflate(R.layout.layout_review_change_safe_owner, container, false)

    override fun onStart() {
        super.onStart()

        disposables += subViewModel.loadAction(transaction)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::setupForm, Timber::e)
    }

    private fun setupForm(action: ChangeSafeSettingsDetailsContract.Action) {
        val (target, description) = when (action) {
            is RemoveOwner -> {
                action.owner to getString(R.string.transaction_description_remove_safe_owner)
            }
            is AddOwner -> {
                action.owner to getString(R.string.transaction_description_add_safe_owner)
            }
            is ReplaceOwner -> {
                action.newOwner to getString(R.string.transaction_description_replace_safe_owner, action.previousOwner)
            }
        }
        layout_review_change_safe_owner_target.text = target
        layout_review_change_safe_owner_action.text = description
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

        fun createInstance(transaction: Transaction?, safeAddress: String?) =
                ReviewChangeDeviceSettingsDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_TRANSACTION, transaction?.parcelable())
                        putString(ARG_SAFE, safeAddress)
                    }
                }
    }

}
