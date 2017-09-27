package pm.gnosis.mnemonic

import pm.gnosis.mnemonic.wordlist.ENGLISH_WORD_LIST
import pm.gnosis.mnemonic.wordlist.WordList

interface Bip39 {
    companion object {
        const val MIN_ENTROPY_BITS = 128
        const val MAX_ENTROPY_BITS = 256
        const val ENTROPY_MULTIPLE = 32
        const val WORD_LIST_SIZE = 2048
    }

    fun normalize(phrase: String): String
    fun salt(password: String?): String
    fun mnemonicToSeed(mnemonic: String, password: String? = null): ByteArray
    fun mnemonicToSeedHex(mnemonic: String, password: String? = null): String
    fun generateMnemonic(strength: Int = MIN_ENTROPY_BITS, wordList: WordList = ENGLISH_WORD_LIST): String
    fun validateMnemonic(mnemonic: String): Boolean
}
