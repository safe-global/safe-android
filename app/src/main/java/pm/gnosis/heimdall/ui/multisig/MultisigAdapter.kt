package pm.gnosis.heimdall.ui.multisig

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_multisig_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.db.MultisigWallet
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext
import pm.gnosis.heimdall.common.util.shareExternalText
import javax.inject.Inject


@ForView
class MultisigAdapter @Inject constructor(@ViewContext private val context: Context) : RecyclerView.Adapter<MultisigAdapter.ViewHolder>() {
    val items = mutableListOf<MultisigWallet>()
    val multisigSelection: PublishSubject<MultisigWallet> = PublishSubject.create<MultisigWallet>()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.fragment_multisig_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder?.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setItems(items: List<MultisigWallet>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
            itemView.fragment_multisig_item_share.setOnClickListener {
                items[adapterPosition].address?.let {
                    context.shareExternalText(it, "Sharing ${items[adapterPosition].name ?: ""}")
                }
            }
        }

        fun bind(item: MultisigWallet) {
            itemView.fragment_multisig_item_address.text = item.address
            itemView.fragment_multisig_item_name.text = item.name
            itemView.fragment_multisig_item_name.visibility = if (item.name.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        override fun onClick(v: View?) {
            multisigSelection.onNext(items[adapterPosition])
        }
    }
}
