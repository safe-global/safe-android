package pm.gnosis.mnemonic

import pm.gnosis.mnemonic.wordlists.WordListProvider

class TestWorldListProvider : WordListProvider {
    companion object {
        const val ENGLISH = 0
        const val CHINESE_TRADITIONAL = 1
        const val CHINESE_SIMPLIFIED = 2

        val MAP = mapOf(
            ENGLISH to ENGLISH_WORD_LIST,
            CHINESE_TRADITIONAL to CHINESE_TRADITIONAL_WORD_LIST,
            CHINESE_SIMPLIFIED to CHINESE_SIMPLIFIED_WORD_LIST
        )
    }

    override fun all() = MAP.values

    override fun get(id: Int) = when (id) {
        ENGLISH -> ENGLISH_WORD_LIST
        CHINESE_TRADITIONAL -> CHINESE_TRADITIONAL_WORD_LIST
        CHINESE_SIMPLIFIED -> CHINESE_SIMPLIFIED_WORD_LIST
        else -> null
    }
}
