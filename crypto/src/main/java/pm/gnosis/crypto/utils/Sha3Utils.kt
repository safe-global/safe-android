package pm.gnosis.crypto.utils

import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.crypto.digests.SHA3Digest
import org.spongycastle.util.encoders.Hex


object Sha3Utils {
    private val DEFAULT_SIZE = 256

    fun sha3String(message: String): String {
        return sha3String(message, SHA3Digest(DEFAULT_SIZE))
    }

    fun sha3String(message: ByteArray): String {
        return hashToString(message, SHA3Digest(DEFAULT_SIZE))
    }

    fun sha3(message: String): ByteArray {
        return hash(Hex.decode(message), SHA3Digest(DEFAULT_SIZE))
    }

    fun sha3(message: ByteArray): ByteArray {
        return hash(message, SHA3Digest(DEFAULT_SIZE))
    }

    fun keccak(message: ByteArray): ByteArray {
        return hash(message, KeccakDigest(DEFAULT_SIZE))
    }

    private fun sha3String(message: String?, digest: SHA3Digest): String {
        if (message != null) {
            return hashToString(Hex.decode(message), digest)
        }
        throw NullPointerException("Can't hash a NULL value")
    }

    private fun hashToString(message: ByteArray, digest: KeccakDigest): String {
        val hash = doDigest(message, digest)
        return Hex.toHexString(hash)
    }

    private fun hash(message: ByteArray, digest: KeccakDigest): ByteArray {
        return doDigest(message, digest)
    }

    private fun doDigest(message: ByteArray, digest: KeccakDigest): ByteArray {
        val hash = ByteArray(digest.digestSize)

        if (message.isNotEmpty()) {
            digest.update(message, 0, message.size)
        }
        digest.doFinal(hash, 0)
        return hash
    }
}