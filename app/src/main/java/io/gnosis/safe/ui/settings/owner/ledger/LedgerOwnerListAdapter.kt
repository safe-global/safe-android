package io.gnosis.safe.ui.settings.owner.ledger

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
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

class LedgerOwnerListAdapter : PagingDataAdapter<OwnerHolder, LedgerOwnerListAdapter.BaseOwnerViewHolder>(COMPARATOR) {

    var pagesVisible = 1
    private var selectedOwnerPosition: Int = -1

    private var listener: WeakReference<OnOwnerItemClickedListener>? = null

    fun setListener(listener: OnOwnerItemClickedListener) {
        this.listener = WeakReference(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
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
            itemCount > 0 -> min(pagesVisible * PAGE_SIZE, itemCount)
            else -> 0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.disabled == true) {
            AccountItemViewType.DISABLED_OWNER.ordinal
        } else {
            AccountItemViewType.OWNER.ordinal
        }
    }


    fun getSelectedOwnerIndex(): Long = selectedOwnerPosition.toLong()

    interface OnOwnerItemClickedListener {
        fun onOwnerClicked(ownerIndex: Long, address: Solidity.Address)
    }

    enum class AccountItemViewType {
        OWNER,
        DISABLED_OWNER
    }

    abstract class BaseOwnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(address: OwnerHolder, position: Int)
    }

    inner class DisabledOwnerViewHolder(private val binding: ItemOwnerSelectionDisabledOwnerBinding) : BaseOwnerViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        override fun bind(ownerHolder: OwnerHolder, position: Int) {
            with(binding) {
                ownerNumber.text = "#${position + 1}"
                // Applying opacity to root.alpha does not immediately grey out the whole item.
                // That is why we set opacity on each element separately
                ownerNumber.alpha = OPACITY_HALF
                ownerImage.setAddress(ownerHolder.address)
                ownerImage.alpha = OPACITY_HALF
                ownerSelection.visible(false)
                ownerSelection.alpha = OPACITY_HALF
                ownerLabel.text = ownerHolder.name
                ownerLabel.alpha = OPACITY_HALF
                ownerShortAddress.text = ownerHolder.address.shortChecksumString()
                ownerShortAddress.alpha = OPACITY_HALF
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
                    listener?.get()?.onOwnerClicked(getSelectedOwnerIndex(), ownerHolder.address)
                }
                ownerNumber.text = "#${position + 1}"
                ownerImage.setAddress(ownerHolder.address)
                ownerAddress.text = ownerHolder.address.formatEthAddress(context = root.context, addMiddleLinebreak = false)
                root.alpha = OPACITY_FULL
                ownerSelection.visible(selectedOwnerPosition == position)
            }
        }
    }

    companion object {

        private const val PAGE_SIZE = LedgerOwnerPagingProvider.PAGE_SIZE

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
