package pm.gnosis.heimdall.ui.addressbook.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_address_book_entry_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.models.AddressBookEntry
import javax.inject.Inject

@ForView
class AddressBookAdapter @Inject constructor() : Adapter<AddressBookEntry, AddressBookAdapter.ViewHolder>() {

    val clicks: PublishSubject<AddressBookEntry> = PublishSubject.create<AddressBookEntry>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_address_book_entry_item, parent, false))

    inner class ViewHolder(itemView: View) : Adapter.ViewHolder<AddressBookEntry>(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        override fun bind(data: AddressBookEntry, payloads: List<Any>) {
            itemView.layout_address_book_entry_item_name.text = data.name
            itemView.layout_address_book_entry_icon.setAddress(data.address)
        }

        override fun onClick(view: View?) {
            clicks.onNext(items[adapterPosition])
        }
    }
}
