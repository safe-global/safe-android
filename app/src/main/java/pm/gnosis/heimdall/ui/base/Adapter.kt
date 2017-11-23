package pm.gnosis.heimdall.ui.base

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import java.util.*


abstract class Adapter<T, VH : Adapter.ViewHolder<T>> : RecyclerView.Adapter<VH>() {
    private var currentDataId: String? = null

    protected val items = mutableListOf<T>()

    override fun getItemCount() = items.size

    abstract override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): VH

    override fun onBindViewHolder(holder: VH?, position: Int, payloads: List<Any>?) {
        holder?.bind(items[position], payloads)
    }

    override fun onBindViewHolder(holder: VH?, position: Int) {
        onBindViewHolder(holder, position, null)
    }

    override fun onViewRecycled(holder: VH?) {
        holder?.unbind()
    }

    fun updateData(data: Data<T>) {
        if (currentDataId == data.id) {
            // This update was already applied
            return
        }
        items.clear()
        items.addAll(data.entries)
        // Make sure that the parent is the one for which the diff was calculated
        if (currentDataId == data.parentId && data.diff != null) {
            data.diff.dispatchUpdatesTo(this)
        } else {
            notifyDataSetChanged()
        }
        currentDataId = data.id
    }

    data class Data<out T>(val parentId: String? = null, val entries: List<T> = emptyList(),
                           val diff: DiffUtil.DiffResult? = null) {
        val id: String = UUID.randomUUID().toString()
    }

    abstract class ViewHolder<in T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(data: T, payloads: List<Any>?)

        open fun unbind() {}
    }
}