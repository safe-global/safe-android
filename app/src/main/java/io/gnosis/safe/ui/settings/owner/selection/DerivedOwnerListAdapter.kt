package io.gnosis.safe.ui.settings.owner.selection

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.databinding.ItemDefaultOwnerDisabledKeyBinding
import io.gnosis.safe.databinding.ItemDefaultOwnerKeyBinding
import io.gnosis.safe.databinding.ItemOwnerSelectionDisabledOwnerBinding
import io.gnosis.safe.databinding.ItemOwnerSelectionOwnerBinding
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_FULL
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_HALF
import io.gnosis.safe.utils.formatEthAddress
import io.gnosis.safe.utils.shortChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import java.lang.ref.WeakReference
import kotlin.math.min

class DerivedOwnerListAdapter() : PagingDataAdapter<OwnerHolder, DerivedOwnerListAdapter.BaseOwnerViewHolder>(COMPARATOR) {

    var pagesVisible = 0
    private var selectedOwnerPosition: Int = 0

    private var listener: WeakReference<OnOwnerItemClickedListener>? = null

    fun setListener(listener: OnOwnerItemClickedListener) {
        this.listener = WeakReference(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        AccountItemViewType.DEFAULT_OWNER.ordinal -> DefaultOwnerViewHolder(
            ItemDefaultOwnerKeyBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
        AccountItemViewType.OWNER.ordinal -> OwnerViewHolder(
            ItemOwnerSelectionOwnerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
        AccountItemViewType.DISABLED_OWNER.ordinal -> DisabledOwnerViewHolder(
            ItemOwnerSelectionDisabledOwnerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
        AccountItemViewType.DISABLED_DEFAULT_OWNER.ordinal -> DisabledDefaultOwnerViewHolder(
            ItemDefaultOwnerDisabledKeyBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
        else -> throw UnsupportedViewType(javaClass.name)
    }

    override fun onBindViewHolder(holder: BaseOwnerViewHolder, position: Int) {
        kotlin.runCatching {
            val uiModel = getItem(position)
            uiModel?.let {
                holder.bind(it, position) //TODO add disable info if already imported
            }
        }
    }

    override fun getItemCount(): Int {
        val itemCount = super.getItemCount()
        return when {
            itemCount > 0 && pagesVisible == 0 -> 1
            itemCount != 0 -> min(pagesVisible * PAGE_SIZE, itemCount)
            else -> 0
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0) {
            if (getItem(position)?.disabled == true) {
                AccountItemViewType.DISABLED_DEFAULT_OWNER.ordinal
            } else {
                AccountItemViewType.DEFAULT_OWNER.ordinal
            }
        } else {
            if (getItem(position)?.disabled == true) {
                AccountItemViewType.DISABLED_OWNER.ordinal
            } else {
                AccountItemViewType.OWNER.ordinal
            }
        }

    fun getSelectedOwnerIndex(): Long = selectedOwnerPosition.toLong()

    interface OnOwnerItemClickedListener {
        fun onOwnerClicked(ownerIndex: Long)
    }

    enum class AccountItemViewType {
        DEFAULT_OWNER,
        OWNER,
        DISABLED_OWNER,
        DISABLED_DEFAULT_OWNER
    }

    abstract class BaseOwnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(address: OwnerHolder, position: Int)
    }

    inner class DisabledOwnerViewHolder(private val binding: ItemOwnerSelectionDisabledOwnerBinding) : BaseOwnerViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(ownerHolder: OwnerHolder, position: Int) {
            with(binding) {
                ownerNumber.text = "#${position + 1}"
                ownerImage.setAddress(ownerHolder.address)
                ownerSelection.visible(selectedOwnerPosition == position)
                ownerLabel.text = ownerHolder.name
                // owners are not bound to a specific chain, so we show addresses without chain prefix
                ownerShortAddress.text = ownerHolder.address.shortChecksumString(chainPrefix = null)
                root.alpha = OPACITY_HALF
            }
        }
    }

    inner class DisabledDefaultOwnerViewHolder(private val binding: ItemDefaultOwnerDisabledKeyBinding) : BaseOwnerViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(ownerHolder: OwnerHolder, position: Int) {
            with(binding) {
                cardContainerLayout.alpha = OPACITY_HALF
                defaultOwnerSelection.visible(false)
                defaultOwnerNumber.text = "#${position + 1}"
                defaultOwnerImage.setAddress(ownerHolder.address)
                // owners are not bound to a specific chain, so we show addresses without chain prefix
                ownerShortAddress.text = ownerHolder.address.shortChecksumString(chainPrefix = null)
                ownerLabel.text = ownerHolder.name
                derivedKeysExplanation.visible(itemCount > 1)
            }
        }
    }

    inner class OwnerViewHolder(private val binding: ItemOwnerSelectionOwnerBinding) : BaseOwnerViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(ownerHolder: OwnerHolder, position: Int) {
            with(binding) {
                root.setOnClickListener {
                    selectedOwnerPosition = position
                    notifyDataSetChanged()
                    listener?.get()?.onOwnerClicked(getSelectedOwnerIndex())
                }
                ownerNumber.text = "#${position + 1}"
                ownerImage.setAddress(ownerHolder.address)
                // owners are not bound to a specific chain, so we show addresses without chain prefix
                ownerAddress.text = ownerHolder.address.formatEthAddress(context = root.context, null, addMiddleLinebreak = false)
                root.alpha = OPACITY_FULL
                ownerSelection.visible(selectedOwnerPosition == position)
            }
        }
    }

    inner class DefaultOwnerViewHolder(private val binding: ItemDefaultOwnerKeyBinding) : BaseOwnerViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(ownerHolder: OwnerHolder, position: Int) {
            with(binding) {
                root.setOnClickListener {
                    selectedOwnerPosition = position
                    notifyDataSetChanged()
                    listener?.get()?.onOwnerClicked(getSelectedOwnerIndex())
                }
                defaultOwnerNumber.text = "#${position + 1}"
                defaultOwnerImage.setAddress(ownerHolder.address)
                // owners are not bound to a specific chain, so we show addresses without chain prefix
                defaultOwnerAddress.text = ownerHolder.address.formatEthAddress(context = root.context, null, addMiddleLinebreak = false)
                defaultOwnerSelection.visibility = if (selectedOwnerPosition == position) View.VISIBLE else View.INVISIBLE
                derivedKeysExplanation.visible(itemCount > 1)
            }
        }
    }

    companion object {

        private const val PAGE_SIZE = DerivedOwnerPagingProvider.PAGE_SIZE

        private val COMPARATOR = object : DiffUtil.ItemCallback<OwnerHolder>() {

            override fun areItemsTheSame(oldItem: OwnerHolder, newItem: OwnerHolder): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: OwnerHolder, newItem: OwnerHolder): Boolean {
                return oldItem == newItem
            }
        }
    }
}

data class OwnerHolder(val address: Solidity.Address, val name: String?, val disabled: Boolean)
