package io.gnosis.safe.ui.settings.owner.ledger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.databinding.ItemLedgerDeviceBinding


class LedgerDeviceListAdapter(private val ownerListener: DeviceListener, private val forSigningOnly: Boolean = false) :
    RecyclerView.Adapter<LedgerDeviceViewHolder>() {

    private val items = mutableListOf<LedgerDeviceViewData>()

    fun updateDeviceData(data: List<LedgerDeviceViewData>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun addDeviceData(deviceViewData: LedgerDeviceViewData) {
        items.add(deviceViewData)
        notifyItemInserted(items.size - 1)
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: LedgerDeviceViewHolder, position: Int) {
        val device = items[position]
        holder.bind(device, ownerListener, position)
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
        fun onDeviceClick()
    }
}

abstract class BaseOwnerViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)

class LedgerDeviceViewHolder(private val viewBinding: ItemLedgerDeviceBinding) : BaseOwnerViewHolder(viewBinding) {

    fun bind(device: LedgerDeviceViewData, deviceListener: LedgerDeviceListAdapter.DeviceListener, position: Int) {
        with(viewBinding) {
            val context = root.context
            deviceItem.name = device.name
            root.setOnClickListener {
                //TODO: pass device data
                deviceListener.onDeviceClick()
            }
        }
    }
}
