package pm.gnosis.mnemonic

import pm.gnosis.mnemonic.wordlists.ENGLISH_WORD_LIST
import pm.gnosis.mnemonic.wordlists.WordList

interface Bip39 {
    companion object {
        const val MIN_ENTROPY_BITS = 128
        const val MAX_ENTROPY_BITS = 256
        const val ENTROPY_MULTIPLE = 32
        const val WORD_LIST_SIZE = 2048
    }

    fun mnemonicToSeed(mnemonic: String, password: String? = null): ByteArray
    fun generateMnemonic(strength: Int = MIN_ENTROPY_BITS, wordList: WordList = ENGLISH_WORD_LIST): String
    fun validateMnemonic(mnemonic: String): String
}

sealed class Bip39ValidationResult : IllegalArgumentException()
data class InvalidEntropy(val mnemonic: String, val entropyBits: Int) : Bip39ValidationResult()
data class InvalidChecksum(val mnemonic: String, val checksum: CharSequence, val generatedChecksum: CharSequence) : Bip39ValidationResult()
data class MnemonicNotInWordlist(val mnemonic: String) : Bip39ValidationResult()
data class EmptyMnemonic(val mnemonic: String) : Bip39ValidationResult()
data class UnknownMnemonicError(val mnemonic: String) : Bip39ValidationResult()
