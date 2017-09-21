package pm.gnosis.mnemonic

import org.spongycastle.jcajce.provider.digest.SHA256
import org.spongycastle.jcajce.provider.symmetric.PBEPBKDF2
import pm.gnosis.mnemonic.wordlist.BIP39_WORDLISTS
import pm.gnosis.mnemonic.wordlist.ENGLISH_WORD_LIST
import pm.gnosis.mnemonic.wordlist.WordList
import pm.gnosis.utils.getIndexesAllMatching
import pm.gnosis.utils.toBinaryString
import pm.gnosis.utils.toHexString
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import java.text.Normalizer
import javax.crypto.SecretKey
import javax.crypto.spec.PBEKeySpec

object Bip39 {
    const val MIN_ENTROPY_BITS = 128
    const val MAX_ENTROPY_BITS = 256
    private const val ENTROPY_MULTIPLE = 32
    private const val WORD_LIST_SIZE = 2048

    class Hasher : PBEPBKDF2.PBKDF2withSHA512() {
        fun generateSecret(keySpec: KeySpec): SecretKey {
            return engineGenerateSecret(keySpec)
        }
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, bytes: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, bytes * 8)
        val skf = Hasher()
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

    fun generateMnemonic(strength: Int = MIN_ENTROPY_BITS, wordList: WordList = ENGLISH_WORD_LIST): String {
        if (strength < MIN_ENTROPY_BITS || strength > MAX_ENTROPY_BITS || strength % ENTROPY_MULTIPLE != 0) {
            throw IllegalArgumentException("Entropy length should be between $MIN_ENTROPY_BITS and $MAX_ENTROPY_BITS and be a multiple of $ENTROPY_MULTIPLE")
        }
        if (wordList.words.size != WORD_LIST_SIZE) throw IllegalArgumentException("Wordlist needs to have $WORD_LIST_SIZE (it has ${wordList.words.size})")

        val bytes = ByteArray(strength / 8)
        SecureRandom().nextBytes(bytes)

        val digest = SHA256.Digest()
        val sha256 = digest.digest(bytes)
        val checksumLength = strength / 32

        val checksum = sha256.toBinaryString().subSequence(0, checksumLength)

        val concatenated = bytes.toBinaryString() + checksum

        val wordIndexes = (0 until concatenated.length step 11).map { concatenated.subSequence(it, it + 11) }.map { Integer.parseInt(it.toString(), 2) }.toList()

        return wordIndexes.joinToString(wordList.separator) { wordList.words[it] }
    }

    fun validateMnemmonic(mnemonic: String): Boolean {
        val words = mnemonic.split(Regex("\\s+"))
        if (words.isEmpty()) return false
        val checksumNBits = (words.size * 11) / (ENTROPY_MULTIPLE + 1)
        val entropyNBits = checksumNBits * 32
        if (entropyNBits % ENTROPY_MULTIPLE != 0 || entropyNBits < MIN_ENTROPY_BITS || entropyNBits > MAX_ENTROPY_BITS) {
            throw IllegalArgumentException("Invalid mnemonic")
        }

        val wordList = BIP39_WORDLISTS.values.firstOrNull { wordList -> wordList.words.contains(words[0]) } ?:
                throw IllegalArgumentException("Invalid mnemonic")

        val binaryIndexes = wordList.words.getIndexesAllMatching(words).joinToString("") { Integer.toBinaryString(it).padStart(11, '0') }

        val checksum = binaryIndexes.subSequence(entropyNBits, binaryIndexes.length)
        val originalEntropy = binaryIndexes.subSequence(0, binaryIndexes.length - checksumNBits)
        val originalBytes = (0 until originalEntropy.length step 8).map { (Integer.valueOf((originalEntropy.subSequence(it, it + 8).toString()), 2) and 0xFF).toByte() }.toByteArray()


        val digest = MessageDigest.getInstance("SHA-256")
        val sha256 = digest.digest(originalBytes)
        val generatedChecksum = sha256.toBinaryString().subSequence(0, checksumNBits)
        return checksum == generatedChecksum
    }
}
