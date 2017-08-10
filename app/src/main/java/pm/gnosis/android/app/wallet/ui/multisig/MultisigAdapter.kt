package pm.gnosis.android.app.wallet.ui.multisig

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_multisig_item.view.*
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.db.MultisigWallet
import pm.gnosis.android.app.wallet.di.ForView
import pm.gnosis.android.app.wallet.di.ViewContext
import javax.inject.Inject


@ForView
class MultisigAdapter @Inject constructor(@ViewContext private val context: Context,
                                          private val presenter: MultisigPresenter) : RecyclerView.Adapter<MultisigAdapter.ViewHolder>() {
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
