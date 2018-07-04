package pm.gnosis.heimdall.ui.safe.create

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ForView
import javax.inject.Inject


@ForView
class ConfirmSafeRecoveryPhraseAdapter @Inject constructor() : RecyclerView.Adapter<ConfirmSafeRecoveryPhraseAdapter.ViewHolder>() {
    private val items = mutableListOf<String>()

    private val dataChanged = PublishSubject.create<List<String>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_confirm_safe_recovery_phrase_item, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(data: String) {
            (itemView as TextView).text = data
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position < 0) return@setOnClickListener
                items.removeAt(position)
                notifyItemRemoved(position)
                dataChanged.onNext(items)
            }
        }
    }

    fun observeWords(): Observable<List<String>> = dataChanged

    fun addWord(word: String) {
        items.add(word)
        notifyItemInserted(items.size - 1)
        dataChanged.onNext(items)
    }
}
