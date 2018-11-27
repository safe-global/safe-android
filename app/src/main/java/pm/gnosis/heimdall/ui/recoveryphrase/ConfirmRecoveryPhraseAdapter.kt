package pm.gnosis.heimdall.ui.recoveryphrase

import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.svalinn.common.utils.getColorCompat
import javax.inject.Inject

@ForView
class ConfirmRecoveryPhraseAdapter @Inject constructor() : RecyclerView.Adapter<ConfirmRecoveryPhraseAdapter.ViewHolder>() {
    private val words = mutableListOf<Word>()
    private val selectedWordCountSubject = PublishSubject.create<Int>()

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

    /**
     * Returns the index of the word that is active or -1 if no word is active
     */
    fun setWords(words: List<String>, inputPositions: List<Int>): Int {
        this.words.clear()
        this.words.addAll(words.mapIndexed { index, word ->
            if (inputPositions.contains(index)) Word(word = "", isSelectable = true, isActive = false, isError = false)
            else Word(word = word, isSelectable = false, isActive = false, isError = false)
        })
        val currentActiveWord = this.words.indexOfFirst { it.isSelectable }
        if (currentActiveWord != -1) {
            this.words[currentActiveWord].isActive = true
        }
        notifyDataSetChanged()
        return currentActiveWord
    }

    /**
     * Returns the index of the word that is active or -1 if no word is active
     */
    fun pushWord(word: String): Int {
        // Set the current active word to inactive
        val currentActiveWordIndex = words.indexOfFirst { it.isActive }
        if (currentActiveWordIndex != -1) {
            words[currentActiveWordIndex].isActive = false
            words[currentActiveWordIndex].word = word
            selectedWordCountSubject.onNext(getSelectedCount())
            notifyItemChanged(currentActiveWordIndex)
        }

        // Set the next word to active if it exists
        val nextActiveWordIndex = words.indexOfFirst { it.isSelectable && it.word.isEmpty() }
        if (nextActiveWordIndex != -1) {
            words[nextActiveWordIndex].isActive = true
            notifyItemChanged(nextActiveWordIndex)
        }
        return nextActiveWordIndex
    }

    /**
     * Returns the word that was popped and the next active position or -1 if no position is active
     */
    fun popWord(): Pair<String, Int> {
        var poppedWord = ""

        // Remove the current active status
        val activeWordIndex = words.indexOfFirst { it.isActive }
        if (activeWordIndex != -1) {
            words[activeWordIndex].isActive = false
            notifyItemChanged(activeWordIndex)
        }

        // Remove the last selectable word and set it to active
        val wordToPopIndex = words.indexOfLast { it.isSelectable && it.word.isNotEmpty() }
        if (wordToPopIndex != -1) {
            poppedWord = words[wordToPopIndex].word
            words[wordToPopIndex].word = ""
            words[wordToPopIndex].isActive = true
            words[wordToPopIndex].isError = false
            selectedWordCountSubject.onNext(getSelectedCount())
            notifyItemChanged(wordToPopIndex)
        }

        return poppedWord to wordToPopIndex
    }

    fun observeSelectedCount(): Observable<Int> = selectedWordCountSubject.startWith(getSelectedCount())

    fun getSelectedCount() = words.count { it.isSelectable && it.word.isNotEmpty() }

    fun getWords() = words.map { it.word }

    fun setIncorrectPositions(incorrectPositions: Set<Int>) {
        incorrectPositions.forEach {
            words[it].isError = true
            notifyItemChanged(it)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(word: Word) {
            (itemView as? TextView)?.apply { text = word.word }
            if (adapterPosition == RecyclerView.NO_POSITION) return
            when (adapterPosition) {
                0 -> updateItem(
                    word = word,
                    inactiveWordResource = R.drawable.ic_inactive_word_first,
                    activeWordResource = R.drawable.ic_active_word_first,
                    outlinedWordResource = R.drawable.ic_outlined_word_first,
                    wrongWordResource = R.drawable.ic_wrong_word_first
                )
                words.size - 1 -> updateItem(
                    word = word,
                    inactiveWordResource = R.drawable.ic_inactive_word_last,
                    activeWordResource = R.drawable.ic_active_word_last,
                    outlinedWordResource = R.drawable.ic_outlined_word_last,
                    wrongWordResource = R.drawable.ic_wrong_word_last
                )
                else -> updateItem(
                    word = word,
                    inactiveWordResource = R.drawable.ic_inactive_word,
                    activeWordResource = R.drawable.ic_active_word,
                    outlinedWordResource = R.drawable.ic_outlined_word,
                    wrongWordResource = R.drawable.ic_wrong_word
                )
            }
        }

        private fun updateItem(
            word: Word,
            @DrawableRes inactiveWordResource: Int,
            @DrawableRes activeWordResource: Int,
            @DrawableRes outlinedWordResource: Int,
            @DrawableRes wrongWordResource: Int
        ) {
            itemView.background = ContextCompat.getDrawable(
                itemView.context,
                if (word.isSelectable) {
                    when {
                        word.isError -> wrongWordResource
                        word.word.isEmpty() -> inactiveWordResource
                        else -> activeWordResource
                    }
                } else inactiveWordResource
            )

            (itemView as? TextView)?.setTextColor(
                itemView.context.getColorCompat(
                    if (word.isSelectable) R.color.word_recovery_phrase_selectable
                    else R.color.word_recovery_phrase
                )
            )

            if (word.isActive) itemView.background = ContextCompat.getDrawable(itemView.context, outlinedWordResource)
        }
    }

    data class Word(var word: String, var isSelectable: Boolean, var isActive: Boolean, var isError: Boolean)

    companion object {
        private const val FIRST_ITEM = 0
        private const val MIDDLE_ITEM = 1
        private const val LAST_ITEM = 2
    }
}
