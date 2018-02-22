package pm.gnosis.heimdall.ui.transactions.details.assets

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
import kotlinx.android.synthetic.main.layout_review_asset_transfer.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.ui.addressbook.helpers.AddressInfoViewHolder
import pm.gnosis.heimdall.ui.base.InflatedViewProvider
import pm.gnosis.heimdall.ui.transactions.details.base.BaseReviewTransactionDetailsFragment
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.stringWithNoTrailingZeroes
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


abstract class ViewAssetTransferDetailsFragment : BaseReviewTransactionDetailsFragment() {

    @Inject
    lateinit var subViewModel: AssetTransferDetailsContract

    var transaction: Transaction? = null
    var safe: BigInteger? = null

    @LayoutRes
    abstract fun layout(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safe = arguments?.getString(ARG_SAFE)?.hexAsBigIntegerOrNull()
        transaction = arguments?.getParcelable<TransactionParcelable>(ARG_TRANSACTION)?.transaction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(layout(), container, false)

    override fun onStart() {
        super.onStart()

        disposables += subViewModel.loadFormData(transaction, false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::setupForm, Timber::e)

        safe?.let {
            disposables += Observable.just(it)
                    .compose(updateSafeInfoTransformer(layout_view_asset_transfer_from))
                    .subscribeBy(onError = Timber::e)
        }
    }

    private fun setupForm(info: AssetTransferDetailsContract.FormData) {
        info.to?.let {
            AddressInfoViewHolder(this, InflatedViewProvider(layout_view_asset_transfer_to_container)).apply {
                bind(it)
            }
        }
        val amount = info.tokenAmount?.let { info.token?.convertAmount(it)?.stringWithNoTrailingZeroes() }
        val tokenSymbol = info.token?.symbol ?: getString(R.string.tokens)
        layout_view_asset_transfer_amount.setText(amount)
        layout_view_asset_transfer_amount.setCurrencySymbol(tokenSymbol)
    }

    override fun observeTransaction(): Observable<Result<Transaction>> {
        return Observable.just(transaction.toOptional())
                // Check that the transaction we are displaying is legit
                .compose(subViewModel.transactionTransformer())
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