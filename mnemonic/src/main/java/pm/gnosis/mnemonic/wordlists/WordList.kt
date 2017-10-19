package pm.gnosis.mnemonic.wordlists

data class WordList(val separator: String, val words: List<String>)

val BIP39_WORDLISTS = mapOf(
        "English" to ENGLISH_WORD_LIST,
        "Chinese Traditional" to CHINESE_TRADITIONAL_WORD_LIST,
        "Chinese Simplified" to CHINESE_SIMPLIFIED_WORD_LIST)
