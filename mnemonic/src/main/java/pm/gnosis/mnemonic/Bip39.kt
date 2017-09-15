package pm.gnosis.mnemonic

import pm.gnosis.mnemonic.wordlist.WordList
import pm.gnosis.mnemonic.wordlist.englishWordList
import pm.gnosis.utils.toBinaryString
import pm.gnosis.utils.toHexString
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.text.Normalizer
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Bip39 {
    const val MIN_ENTROPY_BITS = 128
    const val MAX_ENTROPY_BITS = 256
    private const val ENTROPY_MULTIPLE = 32
    private const val WORD_LIST_SIZE = 2048

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, bytes: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, bytes * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512")
        return skf.generateSecret(spec).encoded
    }

    fun normalize(phrase: String): String {
        return Normalizer.normalize(phrase, Normalizer.Form.NFKD)
    }

    fun salt(password: String?): String {
        return "mnemonic" + (password ?: "")
    }

    fun mnemonicToSeed(mnemonic: String, password: String? = null): ByteArray {
        val mnemonicBuffer = normalize(mnemonic).toCharArray()
        val saltBuffer = salt(normalize(password ?: "")).toByteArray()

        return pbkdf2(mnemonicBuffer, saltBuffer, 2048, 64)
    }

    fun mnemonicToSeedHex(mnemonic: String, password: String? = null): String {
        return mnemonicToSeed(mnemonic, password).toHexString()
    }

    fun generateMnemonic(strength: Int = MIN_ENTROPY_BITS, wordList: WordList = englishWordList): String {
        if (strength < MIN_ENTROPY_BITS || strength > MAX_ENTROPY_BITS || strength % ENTROPY_MULTIPLE != 0) {
            throw IllegalArgumentException("Entropy length should be between $MIN_ENTROPY_BITS and $MAX_ENTROPY_BITS and be a multiple of $ENTROPY_MULTIPLE")
        }
        if (wordList.words.size != WORD_LIST_SIZE) throw IllegalArgumentException("Wordlist needs to have $WORD_LIST_SIZE (it has ${wordList.words.size})")

        val bytes = ByteArray(strength / 8)
        SecureRandom().nextBytes(bytes)

        val digest = MessageDigest.getInstance("SHA-256")
        val sha256 = digest.digest(bytes)
        val checksumLength = strength / 32

        val checksum = sha256.toBinaryString().subSequence(0, checksumLength)

        val concatenated = bytes.toBinaryString() + checksum

        val wordIndexes = (0 until concatenated.length step 11).map { concatenated.subSequence(it, it + 11) }.map { Integer.parseInt(it.toString(), 2) }.toList()

        return wordIndexes.joinToString(wordList.separator) { wordList.words[it] }
    }
}
