package io.gnosis.safe.ui.settings.owner.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemOwnerLocalBinding
import io.gnosis.safe.utils.shortChecksumString
import pm.gnosis.model.Solidity

class OwnerListAdapter(private val ownerListener: OwnerListener) : RecyclerView.Adapter<BaseOwnerViewHolder>() {

    private val items = mutableListOf<OwnerViewData>()

    fun updateData(data: List<OwnerViewData>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onBindViewHolder(holder: BaseOwnerViewHolder, position: Int) {
        when (holder) {
            is LocalOwnerViewHolder -> {
                val owner = items[position] as OwnerViewData.LocalOwner
                holder.bind(owner, ownerListener, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseOwnerViewHolder {
        return when (OwnerItemViewType.values()[viewType]) {
            OwnerItemViewType.LOCAL -> LocalOwnerViewHolder(
                ItemOwnerLocalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when (item) {
            is OwnerViewData.LocalOwner -> OwnerItemViewType.LOCAL
        }.ordinal
    }

    override fun getItemCount() = items.size

    enum class OwnerItemViewType {
        LOCAL
    }

    interface OwnerListener {
        fun onOwnerRemove(owner: Solidity.Address, position: Int)
        fun onOwnerClick(owner: Solidity.Address)
    }
}

abstract class BaseOwnerViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)


class LocalOwnerViewHolder(private val viewBinding: ItemOwnerLocalBinding) : BaseOwnerViewHolder(viewBinding) {

    fun bind(owner: OwnerViewData.LocalOwner, ownerListener: OwnerListAdapter.OwnerListener, position: Int) {
        with(viewBinding) {
            val context = root.context
            blockies.setAddress(owner.address)
            ownerAddress.text = owner.address.shortChecksumString()
            title.text = if (!owner.name.isNullOrBlank()) owner.name else context.getString(R.string.settings_app_imported_owner_key)
            remove.setOnClickListener {
                ownerListener.onOwnerRemove(owner.address, position)
            }
            root.setOnClickListener {
                ownerListener.onOwnerClick(owner.address)
            }
        }
    }
}
