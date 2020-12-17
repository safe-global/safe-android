package io.gnosis.safe.ui.settings.app.fiat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemFiatBinding
import pm.gnosis.svalinn.common.utils.visible
import java.lang.ref.WeakReference
import java.util.*

typealias OnFiatSelected = WeakReference<(fiatCode: String) -> Unit>

class FiatListAdapter(
    private val clickListener: OnFiatSelected,
    private val items: List<String>
) : RecyclerView.Adapter<FiatViewHolder>() {

    init {
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
        FiatViewHolder(
            ItemFiatBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            clickListener
        )

    override fun onBindViewHolder(holder: FiatViewHolder, position: Int) {
        val item = items[position]
        val displayName = Currency.getInstance(item).displayName
        holder.bind(item, displayName, item == selectedItem)
    }

    override fun getItemCount(): Int = items.size
}

class FiatViewHolder(
    private val binding: ItemFiatBinding,
    private val clickListener: OnFiatSelected
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(fiatCode: String, displayName: String, isSelected: Boolean) {
        with(binding) {
            root.setOnClickListener { clickListener.get()?.invoke(fiatCode) }
            fiatCodeText.text = root.context.getString(R.string.separator_hyphen, fiatCode, displayName)
            fiatSelected.visible(isSelected, View.INVISIBLE)
        }
    }
}
