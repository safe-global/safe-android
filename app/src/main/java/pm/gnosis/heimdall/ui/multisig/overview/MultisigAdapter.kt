package pm.gnosis.heimdall.ui.multisig.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_multisig_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext
import pm.gnosis.heimdall.common.util.shareExternalText
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject


@ForView
class MultisigAdapter @Inject constructor(@ViewContext private val context: Context) : Adapter<MultisigWallet, MultisigAdapter.ViewHolder>() {
    val multisigSelection = PublishSubject.create<MultisigWallet>()!!

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.layout_multisig_item, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : Adapter.ViewHolder<MultisigWallet>(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
            itemView.layout_multisig_item_share.setOnClickListener {
                items[adapterPosition].address.asEthereumAddressString().let {
                    context.shareExternalText(it, "Sharing ${items[adapterPosition].name ?: ""}")
                }
            }
        }

        override fun bind(item: MultisigWallet) {
            itemView.layout_multisig_item_address.text = item.address.asEthereumAddressString()
            itemView.layout_multisig_item_name.text = item.name
            itemView.layout_multisig_item_name.visibility = if (item.name.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        override fun onClick(v: View?) {
            multisigSelection.onNext(items[adapterPosition])
        }
    }
}
