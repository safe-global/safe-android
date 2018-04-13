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
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.addressbook.helpers.AddressInfoViewHolder
import pm.gnosis.heimdall.ui.base.InflatedViewProvider
import pm.gnosis.heimdall.ui.transactions.details.base.BaseReviewTransactionDetailsFragment
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.stringWithNoTrailingZeroes
import timber.log.Timber
import javax.inject.Inject

abstract class ViewAssetTransferDetailsFragment : BaseReviewTransactionDetailsFragment() {
    @Inject
    lateinit var subViewModel: AssetTransferDetailsContract

    var transaction: SafeTransaction? = null
    var safe: Solidity.Address? = null

    @LayoutRes
    abstract fun layout(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safe = arguments?.getString(ARG_SAFE)?.asEthereumAddress()
        transaction = arguments?.getParcelable(ARG_TRANSACTION)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(layout(), container, false)

    override fun onStart() {
        super.onStart()

        disposables += subViewModel.loadFormData(transaction?.wrapped, false)
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

    override fun observeTransaction(): Observable<Result<SafeTransaction>> {
        return Observable.just(transaction.toOptional())
            // Check that the wrapped we are displaying is legit
            .compose(subViewModel.transactionTransformer())
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
