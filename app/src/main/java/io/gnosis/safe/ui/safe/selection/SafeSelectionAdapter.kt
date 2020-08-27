package io.gnosis.safe.ui.safe.selection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Safe
import io.gnosis.safe.databinding.ItemAddSafeBinding
import io.gnosis.safe.databinding.ItemSafeBinding
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.utils.abbreviateEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.visible
import java.lang.ref.WeakReference

class SafeSelectionAdapter(
    private val clickListener: WeakReference<OnSafeSelectionItemClickedListener>
) : RecyclerView.Adapter<BaseSafeSelectionViewHolder>() {

    private val items = mutableListOf<Any>()

    var activeSafe: Safe? = null
        set(value) {
            field = value
            notifyAllChanged()
        }

    fun setItems(items: List<Any>, activeSafe: Safe?) {
        this.activeSafe = activeSafe
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BaseSafeSelectionViewHolder, position: Int) {
        when (holder) {
            is AddSafeHeaderViewHolder -> holder.bind()
            is SafeItemViewHolder -> {
                val safe = items[position] as Safe
                holder.bind(safe, safe == activeSafe)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseSafeSelectionViewHolder {
        return when (SafeSelectionViewTypes.values()[viewType]) {
            SafeSelectionViewTypes.HEADER_ADD_SAFE -> AddSafeHeaderViewHolder(
                ItemAddSafeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), clickListener
            )
            SafeSelectionViewTypes.SAFE -> SafeItemViewHolder(
                ItemSafeBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                clickListener
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when (item) {
            is AddSafeHeader -> SafeSelectionViewTypes.HEADER_ADD_SAFE.ordinal
            is Safe -> SafeSelectionViewTypes.SAFE.ordinal
            else -> throw UnsupportedViewType(item.toString())
        }
    }

    override fun getItemCount() = items.size

    fun notifyAllChanged() {
        notifyItemRangeChanged(0, items.size)
    }

    enum class SafeSelectionViewTypes {
        HEADER_ADD_SAFE,
        SAFE
    }

    interface OnSafeSelectionItemClickedListener {
        fun onSafeClicked(safe: Safe)
        fun onAddSafeClicked()
    }
}

abstract class BaseSafeSelectionViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)

object AddSafeHeader

class AddSafeHeaderViewHolder(
    private val binding: ItemAddSafeBinding,
    private val clickListener: WeakReference<SafeSelectionAdapter.OnSafeSelectionItemClickedListener>
) : BaseSafeSelectionViewHolder(binding) {

    fun bind() {
        binding.root.setOnClickListener {
            clickListener.get()?.onAddSafeClicked()
        }
    }
}

class SafeItemViewHolder(
    private val binding: ItemSafeBinding,
    private val clickListener: WeakReference<SafeSelectionAdapter.OnSafeSelectionItemClickedListener>
) : BaseSafeSelectionViewHolder(binding) {

    fun bind(safe: Safe, selected: Boolean) {
        binding.safeName.text = safe.localName
        binding.safeAddress.text = safe.address.asEthereumAddressChecksumString().abbreviateEthAddress()
        binding.safeImage.setAddress(safe.address)
        binding.safeSelection.visible(selected, View.INVISIBLE)
        binding.root.setOnClickListener {
            clickListener.get()?.onSafeClicked(safe)
        }
    }
}
