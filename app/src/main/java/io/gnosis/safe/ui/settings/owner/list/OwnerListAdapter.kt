package io.gnosis.safe.ui.settings.owner.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Owner
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemOwnerLocalBinding
import io.gnosis.safe.utils.imageRes16dp
import io.gnosis.safe.utils.shortChecksumString
import pm.gnosis.model.Solidity

class OwnerListAdapter(private val ownerListener: OwnerListener, private val forSigningOnly: Boolean = false) :
    RecyclerView.Adapter<BaseOwnerViewHolder>() {

    private val items = mutableListOf<OwnerViewData>()

    fun updateData(data: List<OwnerViewData>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        // notifyItemRemoved(position) was not sufficient
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BaseOwnerViewHolder, position: Int) {
        when (holder) {
            is LocalOwnerViewHolder -> {
                val owner = items[position]
                holder.bind(owner, ownerListener, position)
            }
            is LocalOwnerForSigningViewHolder -> {
                val owner = items[position]
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
            OwnerItemViewType.LOCAL_FOR_SIGN -> LocalOwnerForSigningViewHolder(
                ItemOwnerLocalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (forSigningOnly) {
            OwnerItemViewType.LOCAL_FOR_SIGN
        } else {
            OwnerItemViewType.LOCAL
        }.ordinal

    override fun getItemCount() = items.size

    enum class OwnerItemViewType {
        LOCAL,
        LOCAL_FOR_SIGN
    }

    interface OwnerListener {
        fun onOwnerClick(owner: Solidity.Address, type: Owner.Type)
    }
}

abstract class BaseOwnerViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)


class LocalOwnerViewHolder(private val viewBinding: ItemOwnerLocalBinding) : BaseOwnerViewHolder(viewBinding) {

    fun bind(owner: OwnerViewData, ownerListener: OwnerListAdapter.OwnerListener, position: Int) {
        with(viewBinding) {
            val context = root.context
            blockies.setAddress(owner.address)
            keyType.setImageResource(owner.type.imageRes16dp())
            // owners are not bound to a specific chain, so we show addresses without chain prefix
            ownerAddress.text = owner.address.shortChecksumString(chainPrefix = null)
            title.text = if (owner.name.isNullOrBlank())
                context.getString(
                    R.string.settings_app_imported_owner_key_default_name,
                    owner.address.shortChecksumString(chainPrefix = null)
                ) else owner.name
            root.setOnClickListener {
                ownerListener.onOwnerClick(owner.address, owner.type)
            }
        }
    }
}

class LocalOwnerForSigningViewHolder(private val viewBinding: ItemOwnerLocalBinding) : BaseOwnerViewHolder(viewBinding) {

    fun bind(owner: OwnerViewData, ownerListener: OwnerListAdapter.OwnerListener, position: Int) {
        with(viewBinding) {
            val context = root.context
            blockies.setAddress(owner.address)
            keyType.setImageResource(owner.type.imageRes16dp())
            // owners are not bound to a specific chain, so we show addresses without chain prefix
            ownerAddress.text = owner.address.shortChecksumString(chainPrefix = null)
            title.text = if (owner.name.isNullOrBlank())
                context.getString(
                    R.string.settings_app_imported_owner_key_default_name,
                    owner.address.shortChecksumString(chainPrefix = null)
                ) else owner.name
            ownerBalance.text = owner.balance
            root.setOnClickListener {
                ownerListener.onOwnerClick(owner.address, owner.type)
            }
        }
    }
}
