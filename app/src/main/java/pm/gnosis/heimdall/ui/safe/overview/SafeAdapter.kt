package pm.gnosis.heimdall.ui.safe.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_safe_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext
import pm.gnosis.heimdall.common.utils.shareExternalText
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject


@ForView
class SafeAdapter @Inject constructor(@ViewContext private val context: Context) : Adapter<Safe, SafeAdapter.ViewHolder>() {
    val safeSelection = PublishSubject.create<Safe>()!!

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.layout_safe_item, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : Adapter.ViewHolder<Safe>(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
            itemView.layout_safe_item_share.setOnClickListener {
                items[adapterPosition].address.asEthereumAddressString().let {
                    val title = context.getString(R.string.sharing_x, items[adapterPosition].name ?: "")
                    context.shareExternalText(it, title)
                }
            }
        }

        override fun bind(item: Safe, payloads: List<Any>?) {
            itemView.layout_safe_item_address.text = item.address.asEthereumAddressString()
            itemView.layout_safe_item_name.text = item.name
            itemView.layout_safe_item_name.visibility = if (item.name.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        override fun onClick(v: View?) {
            safeSelection.onNext(items[adapterPosition])
        }
    }
}
