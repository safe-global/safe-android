package io.gnosis.safe.ui.settings.chain

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.data.models.Chain
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemChainBinding
import pm.gnosis.svalinn.common.utils.getColorCompat

class ChainSelectionAdapter(
    val mode: ChainSelectionMode
) : PagingDataAdapter<Chain, ChainItemViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ChainItemViewHolder(ItemChainBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ChainItemViewHolder, position: Int) {
        val item = getItem(position)!!
        holder.bind(item, mode)
    }

    companion object {

        private val COMPARATOR = object : DiffUtil.ItemCallback<Chain>() {

            override fun areItemsTheSame(oldItem: Chain, newItem: Chain): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Chain, newItem: Chain): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class ChainItemViewHolder(private val binding: ItemChainBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(chain: Chain, mode: ChainSelectionMode) {
        with(binding) {
            name.text = chain.name
            kotlin.runCatching {
                Color.parseColor(chain.backgroundColor)
            }.onSuccess {
                circle.setColorFilter(it, PorterDuff.Mode.SRC_IN)
            }.onFailure {
                // this should never happen
                circle.setColorFilter(circle.context.getColorCompat(R.color.primary), PorterDuff.Mode.SRC_IN)
            }
            when(mode) {
                ChainSelectionMode.ADD_SAFE -> {
                    root.setOnClickListener {
                        // navigate to add safe screen
                        Navigation.findNavController(it).navigate(
                            ChainSelectionFragmentDirections.actionSelectChainFragmentToAddSafeFragment(chain)
                        )
                    }
                }
                ChainSelectionMode.CHAIN_DETAILS -> {

                }
            }
        }
    }
}
