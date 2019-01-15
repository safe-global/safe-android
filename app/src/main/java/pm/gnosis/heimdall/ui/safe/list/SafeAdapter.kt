package pm.gnosis.heimdall.ui.safe.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.layout_pending_safe_item.view.*
import kotlinx.android.synthetic.main.layout_safe_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import javax.inject.Inject

@ForView
class SafeAdapter @Inject constructor(
    @ViewContext context: Context,
    private val addressHelper: AddressHelper
) : LifecycleAdapter<AbstractSafe, SafeAdapter.CastingViewHolder<out AbstractSafe>>(context) {

    companion object {
        private const val TYPE_SAFE = 0
        private const val TYPE_PENDING_SAFE = 1
        private const val TYPE_RECOVERING_SAFE = 2
    }

    val safeSelection = PublishSubject.create<AbstractSafe>()!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SafeAdapter.CastingViewHolder<out AbstractSafe> {
        return when (viewType) {
            TYPE_SAFE -> {
                ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.layout_safe_item, parent, false),
                    safeSelection,
                    addressHelper
                )
            }
            TYPE_RECOVERING_SAFE, TYPE_PENDING_SAFE -> {
                PendingViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.layout_pending_safe_item, parent, false),
                    safeSelection,
                    addressHelper
                )
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is RecoveringSafe -> TYPE_RECOVERING_SAFE
            is PendingSafe -> TYPE_PENDING_SAFE
            is Safe -> TYPE_SAFE
        }
    }

    abstract class CastingViewHolder<T : AbstractSafe>(val type: Class<T>, itemView: View) : LifecycleViewHolder<AbstractSafe>(itemView) {
        final override fun bind(data: AbstractSafe, payloads: List<Any>) {
            if (type.isInstance(data)) {
                castedBind(type.cast(data), payloads)
            }
        }

        abstract fun castedBind(data: T, payloads: List<Any>)
    }

    class ViewHolder(
        itemView: View,
        private val safeSelection: Subject<AbstractSafe>,
        private val addressHelper: AddressHelper
    ) : CastingViewHolder<Safe>(Safe::class.java, itemView), View.OnClickListener {
        private val disposables = CompositeDisposable()

        private var currentEntry: Safe? = null

        init {
            itemView.setOnClickListener(this)
        }

        override fun castedBind(data: Safe, payloads: List<Any>) {
            currentEntry = data
            itemView.layout_safe_item_address.text = null
            itemView.layout_safe_item_name.text = null
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            currentEntry?.address?.let {
                addressHelper.populateAddressInfo(
                    itemView.layout_safe_item_address,
                    itemView.layout_safe_item_name,
                    itemView.layout_safe_item_image,
                    it
                ).forEach { disposables += it }
            }
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

    class PendingViewHolder(
        itemView: View,
        private val safeSelection: Subject<AbstractSafe>,
        private val addressHelper: AddressHelper
    ) : CastingViewHolder<AbstractSafe>(AbstractSafe::class.java, itemView), View.OnClickListener {
        private val disposables = CompositeDisposable()

        private var currentEntry: AbstractSafe? = null

        init {
            itemView.setOnClickListener(this)
        }

        override fun castedBind(data: AbstractSafe, payloads: List<Any>) {
            currentEntry = when (data) {
                // This layout should only be used for these types of safes
                is PendingSafe, is RecoveringSafe -> data
                else -> null
            }?.apply {
                itemView.layout_pending_safe_item_address.text = null
                itemView.layout_pending_safe_item_name.text = null
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            currentEntry?.address()?.let {
                addressHelper.populateAddressInfo(
                    itemView.layout_pending_safe_item_address,
                    itemView.layout_pending_safe_item_name,
                    null,
                    it
                ).forEach { disposables += it }
            }
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
