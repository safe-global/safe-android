package pm.gnosis.heimdall.ui.addressbook.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_address_book_entry_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext
import pm.gnosis.heimdall.ui.addressbook.detail.AddressBookEntryDetailsActivity
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.models.AddressBookEntry
import javax.inject.Inject

@ForView
class AddressBookAdapter @Inject constructor(
        @ViewContext private val context: Context
) : Adapter<AddressBookEntry, AddressBookAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_address_book_entry_item, parent, false))

    inner class ViewHolder(itemView: View) : Adapter.ViewHolder<AddressBookEntry>(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        override fun bind(data: AddressBookEntry, payloads: List<Any>?) {
            itemView.layout_address_book_entry_item_name.text = data.name
            itemView.layout_address_book_entry_icon.setAddress(data.address)
        }

        override fun onClick(view: View?) {
            context.startActivity(AddressBookEntryDetailsActivity.createIntent(context, items[adapterPosition].address))
        }
    }
}
