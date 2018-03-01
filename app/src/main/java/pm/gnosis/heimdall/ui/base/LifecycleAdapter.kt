package pm.gnosis.heimdall.ui.base

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.view.View
import pm.gnosis.svalinn.common.di.ViewContext

abstract class LifecycleAdapter<T, VH : LifecycleAdapter.LifecycleViewHolder<T>>(
    @ViewContext context: Context
) : Adapter<T, VH>() {
    private val lifecycle = (context as LifecycleOwner).lifecycle

    override fun onBindViewHolder(holder: VH?, position: Int, payloads: List<Any>?) {
        super.onBindViewHolder(holder, position, payloads)
        if (payloads == null || payloads.isEmpty()) {
            // If we have a payload this call is a result of notifyItemChanged
            (holder as? LifecycleObserver)?.let { lifecycle.addObserver(it) }
        }
    }

    override fun onViewRecycled(holder: VH?) {
        (holder as? LifecycleObserver)?.let { lifecycle.removeObserver(it) }
        super.onViewRecycled(holder)
    }

    abstract class LifecycleViewHolder<in T>(itemView: View) : LifecycleObserver, Adapter.ViewHolder<T>(itemView)
}
