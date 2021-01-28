package io.gnosis.safe.ui.settings.owner.list

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.databinding.ItemDefaultOwnerKeyBinding
import io.gnosis.safe.databinding.ItemOwnerSelectionOwnerBinding
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import java.lang.ref.WeakReference
import kotlin.math.min

class OwnerListAdapter() : PagingDataAdapter<Solidity.Address, OwnerListAdapter.BaseOwnerViewHolder>(COMPARATOR) {

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
        else -> throw UnsupportedViewType(javaClass.name)
    }

    override fun onBindViewHolder(holder: BaseOwnerViewHolder, position: Int) {
        kotlin.runCatching {
            val uiModel = getItem(position)
            uiModel?.let {
                holder.bind(it, position)
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
            AccountItemViewType.DEFAULT_OWNER.ordinal
        } else {
            AccountItemViewType.OWNER.ordinal
        }


    private fun getSelectedOwnerIndex(selectedOwnerPosition: Int): Long = selectedOwnerPosition.toLong()

    interface OnOwnerItemClickedListener {
        fun onOwnerClicked(ownerIndex: Long)
    }

    enum class AccountItemViewType {
        DEFAULT_OWNER,
        OWNER
    }

    abstract class BaseOwnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(address: Solidity.Address, position: Int)
    }

    inner class OwnerViewHolder(private val binding: ItemOwnerSelectionOwnerBinding) : BaseOwnerViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        override fun bind(address: Solidity.Address, position: Int) {
            with(binding) {
                root.setOnClickListener {
                    selectedOwnerPosition = position
                    notifyDataSetChanged()
                    listener?.get()?.onOwnerClicked(getSelectedOwnerIndex(selectedOwnerPosition))
                }
                ownerNumber.text = "#${position + 1}"
                ownerImage.setAddress(address)
                ownerAddress.text = address.formatEthAddress(context = root.context, addMiddleLinebreak = false)
                ownerSelection.visible(selectedOwnerPosition == position)
            }
        }
    }

    inner class DefaultOwnerViewHolder(private val binding: ItemDefaultOwnerKeyBinding) : BaseOwnerViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        override fun bind(address: Solidity.Address, position: Int) {
            with(binding) {
                root.setOnClickListener {
                    selectedOwnerPosition = position
                    notifyDataSetChanged()
                    listener?.get()?.onOwnerClicked(getSelectedOwnerIndex(selectedOwnerPosition))
                }
                defaultOwnerNumber.text = "#${position + 1}"
                defaultOwnerImage.setAddress(address)
                defaultOwnerAddress.text = address.formatEthAddress(context = root.context, addMiddleLinebreak = false)
                defaultOwnerSelection.visibility = if (selectedOwnerPosition == position) View.VISIBLE else View.INVISIBLE
                derivedKeysExplanation.visible(itemCount > 1)
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
