package pm.gnosis.heimdall.ui.recoveryphrase

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ForView
import javax.inject.Inject

@ForView
class SetupRecoveryPhraseAdapter @Inject constructor() : RecyclerView.Adapter<SetupRecoveryPhraseAdapter.ViewHolder>() {
    private val words = mutableListOf<String>()

    override fun getItemViewType(position: Int) =
        when (position) {
            0 -> FIRST_ITEM
            words.lastIndex -> LAST_ITEM
            else -> MIDDLE_ITEM
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                when (viewType) {
                    FIRST_ITEM -> R.layout.layout_setup_recovery_phrase_item_first
                    LAST_ITEM -> R.layout.layout_setup_recovery_phrase_item_last
                    MIDDLE_ITEM -> R.layout.layout_setup_recovery_phrase_item
                    else -> throw IllegalStateException("Unknown View Type")
                }, parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(words[position])

    override fun getItemCount() = words.size

    fun setWords(words: List<String>) {
        this.words.clear()
        this.words.addAll(words)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(word: String) {
            (itemView as? TextView)?.apply { text = word }
        }
    }

    companion object {
        private const val FIRST_ITEM = 0
        private const val MIDDLE_ITEM = 1
        private const val LAST_ITEM = 2
    }
}
