package io.gnosis.safe.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseViewHolder<in T>(viewBinding: ViewBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    abstract fun bind(item: T)
}
