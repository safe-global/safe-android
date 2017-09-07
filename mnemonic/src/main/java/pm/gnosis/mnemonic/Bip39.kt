package pm.gnosis.mnemonic

import pm.gnosis.utils.toHexString
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.text.Normalizer
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Bip39 {

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
}