package io.gnosis.safe.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class BaseFactory<VH : BaseViewHolder<*>> {

    abstract fun newViewHolder(viewBinding: ViewBinding, viewType: Int): VH

    abstract fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding

    open fun <T> viewTypeFor(item: T): Int = 0
}

class UnsupportedViewType(message: String? = null) : Throwable(message)
