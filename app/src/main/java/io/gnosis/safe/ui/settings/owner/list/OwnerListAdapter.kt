package io.gnosis.safe.ui.settings.owner.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemOwnerLocalBinding
import io.gnosis.safe.utils.shortChecksumString
import io.gnosis.safe.utils.showConfirmDialog
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar

class OwnerListAdapter : RecyclerView.Adapter<BaseOwnerViewHolder>() {

    private val items = mutableListOf<OwnerViewData>()

    fun updateData(data: List<OwnerViewData>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BaseOwnerViewHolder, position: Int) {
        when (holder) {
            is LocalOwnerViewHolder -> {
                val owner = items[position] as OwnerViewData.LocalOwner
                holder.bind(owner)
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
}

abstract class BaseOwnerViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)


class LocalOwnerViewHolder(private val viewBinding: ItemOwnerLocalBinding) : BaseOwnerViewHolder(viewBinding) {

    fun bind(owner: OwnerViewData.LocalOwner) {
        with(viewBinding) {
            val context = root.context
            blockies.setAddress(owner.address)
            ownerAddress.text = owner.address.shortChecksumString()
            title.text = if (!owner.name.isNullOrBlank()) owner.name else context.getString(R.string.settings_app_imported_owner_key)
            remove.setOnClickListener {
                showConfirmDialog(context, R.string.signing_owner_dialog_description) {
                   // onOwnerRemove()
                }
            }
            root.setOnClickListener {
                owner.address.let {
                    context?.copyToClipboard(context.getString(R.string.address_copied), owner.address.asEthereumAddressChecksumString()) {
                        //     snackbar(view = root, textId = R.string.copied_success)
                    }
                }
            }
        }
    }
}
