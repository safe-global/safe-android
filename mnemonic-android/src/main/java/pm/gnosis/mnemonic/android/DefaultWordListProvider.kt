package pm.gnosis.mnemonic.android

import android.content.Context
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.mnemonic.wordlists.WordList
import pm.gnosis.mnemonic.wordlists.WordListProvider
import javax.inject.Inject

class DefaultWordListProvider @Inject constructor(
        @ApplicationContext private val context: Context
) : WordListProvider {
    companion object {
        val supportedWordList = mapOf(
                R.id.english to R.raw.english,
                R.id.chinese_simplified to R.raw.chinese_simplified,
                R.id.chinese_traditional to R.raw.chinese_traditional)
    }

    private val cache = mutableMapOf<Int, WordList?>()

    override fun all() = supportedWordList
            .mapNotNull { (languageId, _) -> get(languageId) }.toCollection(mutableListOf())

    override fun get(id: Int) = cache.getOrElse(id, { readWordlistFromResource(id)?.also { cache[id] = it } })

    private fun readWordlistFromResource(id: Int): WordList? = readWordsFromResource(id)?.let { words ->
        when (id) {
            R.id.english -> WordList(" ", words)
            R.id.chinese_simplified -> WordList(" ", words)
            R.id.chinese_traditional -> WordList(" ", words)
            else -> null
        }
    }

    private fun readWordsFromResource(id: Int): List<String>? = supportedWordList[id]?.let { resource ->
        context.resources.openRawResource(resource).bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }
    }
}
