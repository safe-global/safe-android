package io.gnosis.safe.ui.safe.selection

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemAddSafeBinding
import io.gnosis.safe.databinding.ItemChainHeaderBinding
import io.gnosis.safe.databinding.ItemSafeBinding
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewData.*
import io.gnosis.safe.utils.abbreviateEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import java.lang.ref.WeakReference

class SafeSelectionAdapter(
    private val clickListener: WeakReference<OnSafeSelectionItemClickedListener>
) : RecyclerView.Adapter<BaseSafeSelectionViewHolder>() {

    private val items = mutableListOf<SafeSelectionViewData>()
    private var showChainPrefix: Boolean = false

    var activeSafe: Safe? = null
        set(value) {
            field = value
            notifyAllChanged()
        }

    fun setItems(items: List<SafeSelectionViewData>, activeSafe: Safe?, showChainPrefix: Boolean) {
        this.activeSafe = activeSafe
        this.items.clear()
        this.items.addAll(items)
        this.showChainPrefix = showChainPrefix
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BaseSafeSelectionViewHolder, position: Int) {
        when (holder) {
            is AddSafeHeaderViewHolder -> holder.bind()
            is ChainHeaderViewHolder -> {
                val chainHeader = items[position] as ChainHeader
                holder.bind(chainHeader)
            }
            is SafeItemViewHolder -> {
                val safeItem = items[position] as SafeItem
                holder.bind(safeItem.safe, safeItem.safe == activeSafe, showChainPrefix)
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
            SafeSelectionViewTypes.HEADER_СHAIN -> ChainHeaderViewHolder(
                ItemChainHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
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
            is ChainHeader -> SafeSelectionViewTypes.HEADER_СHAIN.ordinal
            is SafeItem -> SafeSelectionViewTypes.SAFE.ordinal
            else -> throw UnsupportedViewType(item.toString())
        }
    }

    override fun getItemCount() = items.size

    fun notifyAllChanged() {
        notifyItemRangeChanged(0, items.size)
    }

    enum class SafeSelectionViewTypes {
        HEADER_ADD_SAFE,
        HEADER_СHAIN,
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

class ChainHeaderViewHolder(
    private val binding: ItemChainHeaderBinding
) : BaseSafeSelectionViewHolder(binding) {

    fun bind(chainHeader: ChainHeader) {
        with(binding) {
            kotlin.runCatching {
                Color.parseColor(chainHeader.color)
            }.onSuccess {
                chainCircle.setColorFilter(it, PorterDuff.Mode.SRC_IN)
            }.onFailure {
                // this should never happen
                chainCircle.setColorFilter(
                    chainCircle.context.getColorCompat(R.color.primary),
                    PorterDuff.Mode.SRC_IN
                )
            }
            chainName.text = chainHeader.name
        }
    }
}

class SafeItemViewHolder(
    private val binding: ItemSafeBinding,
    private val clickListener: WeakReference<SafeSelectionAdapter.OnSafeSelectionItemClickedListener>
) : BaseSafeSelectionViewHolder(binding) {

    fun bind(safe: Safe, selected: Boolean, showChainPrefix: Boolean) {
        with(binding) {
            safeName.text = safe.localName
            safeAddress.text = safe.address.asEthereumAddressChecksumString()
                .abbreviateEthAddress(if (showChainPrefix) safe.chain.shortName else null)
            safeImage.setAddress(safe.address)
            safeSelection.visible(selected, View.INVISIBLE)
            root.setOnClickListener {
                clickListener.get()?.onSafeClicked(safe)
            }
        }
    }
}
