package io.gnosis.safe.ui.settings.owner.ledger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.databinding.ItemLedgerDeviceBinding


class LedgerDeviceListAdapter(private val listener: DeviceListener) :
    RecyclerView.Adapter<LedgerDeviceViewHolder>() {

    private val items = mutableListOf<LedgerDeviceViewData>()

    fun updateDeviceData(data: List<LedgerDeviceViewData>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: LedgerDeviceViewHolder, position: Int) {
        val device = items[position]
        holder.bind(device, listener, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerDeviceViewHolder {
        return LedgerDeviceViewHolder(
            ItemLedgerDeviceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount() = items.size

    interface DeviceListener {
        fun onDeviceClick(position: Int)
    }
}

abstract class BaseOwnerViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)

class LedgerDeviceViewHolder(private val viewBinding: ItemLedgerDeviceBinding) : BaseOwnerViewHolder(viewBinding) {

    fun bind(device: LedgerDeviceViewData, listener: LedgerDeviceListAdapter.DeviceListener, position: Int) {
        with(viewBinding) {
            deviceItem.name = device.name
            root.setOnClickListener {
                //TODO: pass device data
                listener.onDeviceClick(position)
            }
        }
    }
}
