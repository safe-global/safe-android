package io.gnosis.safe.ui.settings.app.fiat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemFiatBinding
import pm.gnosis.svalinn.common.utils.visible
import java.util.*

typealias OnFiatSelected = (fiatCode: String) -> Unit

class FiatListAdapter : RecyclerView.Adapter<FiatListAdapter.FiatViewHolder>() {

    var clickListener: OnFiatSelected? = null
    val items = mutableListOf<String>()

    fun setItems(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    var selectedItem: String? = null
        set(value) {
            val oldValueIndex = items.indexOf(field)
            val newValueIndex = items.indexOf(value)
            field = value
            notifyItemChanged(oldValueIndex)
            notifyItemChanged(newValueIndex)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FiatViewHolder =
        FiatViewHolder(ItemFiatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: FiatViewHolder, position: Int) {
        val item = items[position]
        val displayName = Currency.getInstance(item).getDisplayName(Locale("en", Locale.getDefault().country))
        holder.bind(item, displayName, item == selectedItem)
    }

    override fun getItemCount(): Int = items.size

    inner class FiatViewHolder(
        private val binding: ItemFiatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { clickListener?.invoke(items[absoluteAdapterPosition]) }
        }

        fun bind(fiatCode: String, displayName: String, isSelected: Boolean) {
            with(binding) {
                fiatCodeText.text = root.context.getString(R.string.separator_hyphen, fiatCode, displayName)
                fiatSelected.visible(isSelected, View.INVISIBLE)
            }
        }
    }
}
