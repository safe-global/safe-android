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
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_pending_safe_item.view.*
import kotlinx.android.synthetic.main.layout_safe_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext
import pm.gnosis.heimdall.common.utils.shareExternalText
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.asTransactionHash
import timber.log.Timber
import javax.inject.Inject


@ForView
class SafeAdapter @Inject constructor(
        @ViewContext private val context: Context,
        private val safeRepository: GnosisSafeRepository
) : LifecycleAdapter<AbstractSafe, SafeAdapter.CastingViewHolder<out AbstractSafe>>(context) {

    companion object {
        private const val TYPE_PENDING_SAFE = 0
        private const val TYPE_SAFE = 1
    }

    val safeSelection = PublishSubject.create<Safe>()!!

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): SafeAdapter.CastingViewHolder<out AbstractSafe> {
        return when (viewType) {
            TYPE_SAFE -> {
                ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_safe_item, parent, false))
            }
            TYPE_PENDING_SAFE -> {
                PendingViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_pending_safe_item, parent, false))
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

    inner abstract class CastingViewHolder<T : AbstractSafe>(val type: Class<T>, itemView: View) : LifecycleViewHolder<AbstractSafe>(itemView) {
        override final fun bind(data: AbstractSafe, payloads: List<Any>?) {
            if (type.isInstance(data)) {
                castedBind(type.cast(data), payloads)
            }
        }

        abstract fun castedBind(data: T, payloads: List<Any>?)
    }

    inner class ViewHolder(itemView: View) : CastingViewHolder<Safe>(Safe::class.java, itemView), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
            itemView.layout_safe_item_share.setOnClickListener {
                currentEntry?.let {
                    it.let {
                        val addressString = it.address.asEthereumAddressString()
                        val title = context.getString(R.string.sharing_x, it.name ?: addressString)
                        context.shareExternalText(addressString, title)
                    }
                }
            }
        }

        private var currentEntry: Safe? = null

        override fun castedBind(data: Safe, payloads: List<Any>?) {
            currentEntry = data
            itemView.layout_safe_item_address.text = data.address.asEthereumAddressString()
            itemView.layout_safe_item_name.text = data.name
            itemView.layout_safe_item_name.visibility = if (data.name.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        override fun unbind() {
            currentEntry = null
            super.unbind()
        }

        override fun onClick(v: View?) {
            currentEntry?.let { safeSelection.onNext(it) }
        }
    }

    inner class PendingViewHolder(itemView: View) : CastingViewHolder<PendingSafe>(PendingSafe::class.java, itemView) {

        private val disposables = CompositeDisposable()

        private var currentEntry: PendingSafe? = null

        override fun castedBind(data: PendingSafe, payloads: List<Any>?) {
            currentEntry = data
            itemView.layout_pending_safe_item_name.text = data.name
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            val pendingSafe = currentEntry ?: return
            disposables += safeRepository.observeDeployStatus(pendingSafe.hash.asTransactionHash())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::safeDeployed, Timber::e)

        }

        private fun safeDeployed(safeAddress: String) {
            Timber.i("#######", safeAddress)
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
    }
}
