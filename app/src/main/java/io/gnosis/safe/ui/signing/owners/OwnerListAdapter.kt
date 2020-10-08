package io.gnosis.safe.ui.signing.owners

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.databinding.ItemOwnerSelectionHeaderBinding
import io.gnosis.safe.databinding.ItemOwnerSelectionOwnerBinding
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import java.lang.ref.WeakReference

class OwnerListAdapter() : PagingDataAdapter<Solidity.Address, RecyclerView.ViewHolder>(COMPARATOR) {

    var pagesVisible = 1
    private var selectedOwnerPosition: Int = 1

    private var listener: WeakReference<OnOwnerItemClickedListener>? = null

    fun setListener(listener: OnOwnerItemClickedListener) {
        this.listener = WeakReference(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        AccountItemViewType.HEADER.ordinal -> HeaderViewHolder(
                ItemOwnerSelectionHeaderBinding.inflate(
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
        else -> throw UnsupportedViewType(javaClass.name)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is OwnerViewHolder) {
            kotlin.runCatching {
                val uiModel = getItem(position)
                uiModel?.let {
                    holder.bind(it, position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        val itemCount = super.getItemCount()
        return if (itemCount != 0)
            Math.min(pagesVisible * PAGE_SIZE + 1, itemCount)
        else 0
    }

    override fun getItemViewType(position: Int): Int = when {
        position == 0 -> AccountItemViewType.HEADER.ordinal
        else -> AccountItemViewType.OWNER.ordinal
    }

    private fun getSelectedOwnerIndex(selectedOwnerPosition: Int): Long {
        return selectedOwnerPosition.toLong() - 1 //account for header
    }

    interface OnOwnerItemClickedListener {
        fun onOwnerClicked(ownerIndex: Long)
    }

    enum class AccountItemViewType {
        HEADER,
        OWNER
    }

    class HeaderViewHolder(private val binding: ItemOwnerSelectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    inner class OwnerViewHolder(private val binding: ItemOwnerSelectionOwnerBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(address: Solidity.Address, position: Int) {
            Timber.i("---> Position: $position")
            with(binding) {
                root.setOnClickListener {
                    selectedOwnerPosition = position
                    notifyDataSetChanged()
                    Timber.i("---> setOnClickListener clicked")

                    listener?.get()?.onOwnerClicked(getSelectedOwnerIndex(selectedOwnerPosition))
                }
                ownerNumber.text = "#$position"
                ownerImage.setAddress(address)
                ownerAddress.text = address.formatEthAddress(context = root.context, addMiddleLinebreak = false)
                ownerSelection.visible(selectedOwnerPosition == position)
            }
        }
    }

    companion object {

        private const val PAGE_SIZE = OwnerPagingProvider.PAGE_SIZE

        private val COMPARATOR = object : DiffUtil.ItemCallback<Solidity.Address>() {

            override fun areItemsTheSame(oldItem: Solidity.Address, newItem: Solidity.Address): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Solidity.Address, newItem: Solidity.Address): Boolean {
                return oldItem == newItem
            }
        }
    }
}
