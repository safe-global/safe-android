package io.gnosis.safe.ui.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class MultiViewHolderAdapter<VH, T>(
    private val factory: BaseFactory<VH, T>
) : Adapter<T, VH>() where VH : Adapter.ViewHolder<T> {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        factory.newViewHolder(
            factory.layout(LayoutInflater.from(parent.context), parent, viewType),
            viewType
        )

    override fun getItemViewType(position: Int): Int =
        factory.viewTypeFor(items[position]).takeUnless { it < 0 } ?: throw UnsupportedViewType()
}

abstract class BaseFactory<VH : Adapter.ViewHolder<T>, T> {

    abstract fun newViewHolder(viewBinding: ViewBinding, viewType: Int): VH

    abstract fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding

    open fun viewTypeFor(item: T): Int = 0
}
