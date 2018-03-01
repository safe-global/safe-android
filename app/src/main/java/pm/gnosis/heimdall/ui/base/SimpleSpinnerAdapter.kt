package pm.gnosis.heimdall.ui.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_simple_spinner_item.view.*
import pm.gnosis.heimdall.R

abstract class SimpleSpinnerAdapter<T>(context: Context) : ArrayAdapter<T>(context, R.layout.layout_simple_spinner_item, ArrayList()) {

    abstract fun title(item: T): String?
    abstract fun subTitle(item: T): String?

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?) =
        getDropDownView(position, convertView, parent).apply {
            setPadding(0, 0, 0, 0)
        }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder = getViewHolder(convertView, parent)
        val item = getItem(position)
        viewHolder.titleText.text = title(item)
        viewHolder.subtitleText.text = subTitle(item)
        return viewHolder.itemView
    }

    private fun getViewHolder(convertView: View?, parent: ViewGroup?): ViewHolder {
        val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.layout_simple_spinner_item, parent, false)
        return (view.tag as? ViewHolder) ?: createAndSetViewHolder(view)
    }

    private fun createAndSetViewHolder(view: View) =
        ViewHolder(view, view.layout_simple_spinner_item_name, view.layout_simple_spinner_item_address)
            .apply { view.tag = this }

    data class ViewHolder(val itemView: View, val titleText: TextView, val subtitleText: TextView)
}
