package io.gnosis.safe.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class BaseAdapter<VH, T>(
    private val factory: BaseFactory<VH>
) : RecyclerView.Adapter<VH>() where VH : BaseViewHolder<T> {

    private val items = mutableListOf<T>()

    @Deprecated("Unsafe")
    fun setItemsUnsafe(items: List<T>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        factory.newViewHolder(
            factory.layout(LayoutInflater.from(parent.context), parent, viewType),
            viewType
        )

    override fun getItemViewType(position: Int): Int =
        factory.viewTypeFor(items[position]).takeUnless { it < 0 } ?: throw UnsupportedItem()


    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    fun notifyAllChanged() {
        notifyItemRangeChanged(0, items.size)
    }

    fun notifyChangedAt(position: Int) {
        notifyItemChanged(position)
    }

    fun appendItems(items: List<T>) {
        val oldSize = items.size
        this.items.addAll(items)
        notifyItemRangeChanged(oldSize, this.items.size)
    }
}

class UnsupportedItem : Throwable()
