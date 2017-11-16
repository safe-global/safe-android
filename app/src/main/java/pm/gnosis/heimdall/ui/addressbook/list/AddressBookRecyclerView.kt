package pm.gnosis.heimdall.ui.addressbook.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_address_book_entry_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.ui.addressbook.detail.AddressBookEntryDetailsActivity
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.initials
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.utils.asEthereumAddressStringOrNull
import javax.inject.Inject

@ForView
class AddressBookRecyclerView @Inject constructor(
        @ApplicationContext private val context: Context
) : Adapter<AddressBookEntry, AddressBookRecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_address_book_entry_item, parent, false))


    inner class ViewHolder(itemView: View) : Adapter.ViewHolder<AddressBookEntry>(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        override fun bind(data: AddressBookEntry, payloads: List<Any>?) {
            itemView.layout_address_book_entry_item_name.text = data.name
            itemView.layout_address_book_entry_initials.text = data.name.initials().toUpperCase()
        }

        override fun onClick(view: View?) {
            val address = items[adapterPosition].address.asEthereumAddressStringOrNull()
            if (address == null) {
                context.toast(R.string.invalid_ethereum_address)
            } else {
                context.startActivity(AddressBookEntryDetailsActivity.createIntent(context, address))
            }
        }
    }
}
