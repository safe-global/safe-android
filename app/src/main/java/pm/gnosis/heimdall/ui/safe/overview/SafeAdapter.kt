package pm.gnosis.heimdall.ui.safe.overview

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_pending_safe_item.view.*
import kotlinx.android.synthetic.main.layout_safe_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.asTransactionHash
import timber.log.Timber
import javax.inject.Inject

@ForView
class SafeAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val viewModel: SafeOverviewContract
) : LifecycleAdapter<AbstractSafe, SafeAdapter.CastingViewHolder<out AbstractSafe>>(context) {

    companion object {
        private const val TYPE_PENDING_SAFE = 0
        private const val TYPE_SAFE = 1
    }

    val safeSelection = PublishSubject.create<AbstractSafe>()!!
    val shareSelection = PublishSubject.create<Solidity.Address>()!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SafeAdapter.CastingViewHolder<out AbstractSafe> {
        return when (viewType) {
            TYPE_SAFE -> {
                ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_safe_item, parent, false))
            }
            TYPE_PENDING_SAFE -> {
                PendingViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_pending_safe_item, parent, false))
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PendingSafe -> TYPE_PENDING_SAFE
            is Safe -> TYPE_SAFE
        }
    }

    abstract inner class CastingViewHolder<T : AbstractSafe>(val type: Class<T>, itemView: View) : LifecycleViewHolder<AbstractSafe>(itemView) {
        final override fun bind(data: AbstractSafe, payloads: List<Any>) {
            if (type.isInstance(data)) {
                castedBind(type.cast(data), payloads)
            }
        }

        abstract fun castedBind(data: T, payloads: List<Any>)
    }

    inner class ViewHolder(itemView: View) : CastingViewHolder<Safe>(Safe::class.java, itemView), View.OnClickListener {
        private val disposables = CompositeDisposable()

        private var currentEntry: Safe? = null

        init {
            itemView.setOnClickListener(this)
            itemView.layout_safe_item_share.setOnClickListener {
                currentEntry?.let { shareSelection.onNext(it.address) }
            }
        }

        override fun castedBind(data: Safe, payloads: List<Any>) {
            currentEntry = data
            itemView.layout_safe_item_address.text = data.address.asEthereumAddressString()
            itemView.layout_safe_item_name.text = data.name
            itemView.layout_safe_item_name.visibility = if (data.name.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            currentEntry?.address?.let { address ->
                disposables += viewModel.loadSafeInfo(address)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onSuccess = ::onSafeInfo, onError = Timber::e)
            }
        }

        private fun onSafeInfo(safeInfo: SafeInfo) {
            itemView.layout_safe_item_authorizations.text = "${safeInfo.requiredConfirmations}/${safeInfo.owners.count()}"
            itemView.layout_safe_item_ether.text = safeInfo.balance.displayString(context)
            itemView.layout_safe_item_owner.visibility = if (safeInfo.isOwner) View.VISIBLE else View.GONE
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {
            disposables.clear()
        }

        override fun unbind() {
            currentEntry = null
            super.unbind()
        }

        override fun onClick(v: View?) {
            currentEntry?.let { safeSelection.onNext(it) }
        }
    }

    inner class PendingViewHolder(itemView: View) : CastingViewHolder<PendingSafe>(PendingSafe::class.java, itemView), View.OnClickListener {

        private val disposables = CompositeDisposable()

        private var currentEntry: PendingSafe? = null

        init {
            itemView.setOnClickListener(this)
        }

        override fun castedBind(data: PendingSafe, payloads: List<Any>) {
            currentEntry = data
            itemView.layout_pending_safe_item_name.text = data.name
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            val pendingSafe = currentEntry ?: return
            disposables += viewModel.observeDeployStatus(pendingSafe.hash.asTransactionHash())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = {
                    context.toast(R.string.error_deploying_safe)
                    Timber.e(it)
                })
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {
            disposables.clear()
        }

        override fun unbind() {
            stop()
            currentEntry = null
            super.unbind()
        }

        override fun onClick(v: View?) {
            currentEntry?.let { safeSelection.onNext(it) }
        }
    }
}
