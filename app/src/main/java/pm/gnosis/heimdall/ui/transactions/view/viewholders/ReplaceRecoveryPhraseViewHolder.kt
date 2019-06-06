package pm.gnosis.heimdall.ui.transactions.view.viewholders

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_replace_recovery_phrase_transaction_info.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.model.Solidity

class ReplaceRecoveryPhraseViewHolder(
    private val addressHelper: AddressHelper,
    private val safeAddress: Solidity.Address,
    private val safeTransaction: SafeTransaction
) : TransactionInfoViewHolder {
    private var view: View? = null
    private val disposables = CompositeDisposable()

    override fun loadTransaction(): Single<SafeTransaction> = Single.just(safeTransaction)

    override fun loadAssetChange(): Single<Optional<ERC20TokenWithBalance>> = Single.just(None)

    override fun inflate(inflater: LayoutInflater, root: ViewGroup) {
        view = inflater.inflate(R.layout.layout_replace_recovery_phrase_transaction_info, root, true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        val view = view ?: return

        addressHelper.populateAddressInfo(
            addressView = view.layout_replace_recovery_phrase_transaction_info_safe_address,
            nameView = view.layout_replace_recovery_phrase_transaction_info_safe_name,
            imageView = view.layout_replace_recovery_phrase_transaction_info_safe_image,
            address = safeAddress
        ).forEach {
            disposables += it
        }
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
