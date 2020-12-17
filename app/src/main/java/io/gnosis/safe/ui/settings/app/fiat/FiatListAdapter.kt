package io.gnosis.safe.ui.settings.app.fiat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.databinding.ItemFiatBinding
import pm.gnosis.svalinn.common.utils.visible
import java.lang.ref.WeakReference


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
        holder.bind(item, item == selectedItem)
    }

    override fun getItemCount(): Int = items.size
}

class FiatViewHolder(
    private val binding: ItemFiatBinding,
    private val clickListener: OnFiatSelected
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(fiatCode: String, isSelected: Boolean) {
        with(binding) {
            root.setOnClickListener { clickListener.get()?.invoke(fiatCode) }
            fiatCodeText.text = fiatCode
            fiatSelected.visible(isSelected, View.INVISIBLE)
        }
    }
}
