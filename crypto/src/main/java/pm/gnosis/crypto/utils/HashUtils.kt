package pm.gnosis.crypto.utils

import java.util.*


object HashUtils {
    fun sha3lower20(input: ByteArray): ByteArray {
        val hash = Sha3Utils.keccak(input)
        return Arrays.copyOfRange(hash, 12, hash.size)
    }
}