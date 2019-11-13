package pm.gnosis.heimdall.ui.transactions.view.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_multi_send_transaction_info.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.builder.MultiSendTransactionBuilder
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.details.MultiSendDetailsActivity
import pm.gnosis.model.Solidity
import timber.log.Timber

class MultiSendViewHolder(
    private val addressHelper: AddressHelper,
    private val data: TransactionData.MultiSend,
    private val safe: Solidity.Address
) : TransactionInfoViewHolder {

    private var view: View? = null
    private val disposables = CompositeDisposable()

    override fun loadTransaction(): Single<SafeTransaction> =
        Single.fromCallable {
            MultiSendTransactionBuilder.build(data)
        }.subscribeOn(Schedulers.computation())

    override fun loadAssetChange(): Single<Optional<ERC20TokenWithBalance>> = Single.just(None)

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        val view = view ?: return

        view.multi_send_transaction_info_safe_image.setAddress(safe)
        view.multi_send_transaction_info_description.text =
            view.context.getString(R.string.this_will_perform_x_transactions, data.transactions.size.toString())
        disposables +=
            addressHelper.buildAddressInfoSingle(
                addressView = view.multi_send_transaction_info_safe_address,
                nameView = view.multi_send_transaction_info_safe_name,
                address = safe
            )
                .subscribeBy(onError = Timber::e)

        view.multi_send_transaction_info_details_btn.setOnClickListener {
            view.context.startActivity(
                MultiSendDetailsActivity.createIntent(
                    view.context,
                    safe,
                    data
                )
            )
        }
    }

    override fun inflate(inflater: LayoutInflater, root: ViewGroup) {
        view = inflater.inflate(R.layout.layout_multi_send_transaction_info, root, true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        disposables.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    override fun detach() {
        view = null
    }
}
