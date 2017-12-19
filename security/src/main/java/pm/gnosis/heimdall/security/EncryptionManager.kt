package pm.gnosis.heimdall.security

import io.reactivex.Single
import pm.gnosis.utils.hexStringToByteArrayOrNull
import pm.gnosis.utils.toHexString


interface EncryptionManager {
    fun decrypt(data: CryptoData): ByteArray
    fun encrypt(data: ByteArray): CryptoData
    fun unlocked(): Single<Boolean>
    fun unlockWithPassword(keyBytes: ByteArray): Single<Boolean>
    fun lock()
    fun setupPassword(newKeyBytes: ByteArray, oldKeyBytes: ByteArray? = null): Single<Boolean>
    fun initialized(): Single<Boolean>

    class CryptoData(val data: ByteArray, val iv: ByteArray) {
        override fun toString(): String {
            return "${data.toHexString()}$SEPARATOR${iv.toHexString()}"
        }

        companion object {

            const val SEPARATOR = "####"
            fun fromString(encoded: String) =
                    encoded.split(SEPARATOR).let {
                        if (it.size != 2) throw IllegalArgumentException("Not correctly encoded!")
                        val data = it[0].hexStringToByteArrayOrNull() ?: throw IllegalArgumentException("Could not decode data!")
                        val iv = it[1].hexStringToByteArrayOrNull() ?: throw IllegalArgumentException("Could not decode iv!")
                        CryptoData(data, iv)
                    }
        }
    }
}
