package pm.gnosis.crypto.utils

import okio.ByteString
import java.lang.AssertionError
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


fun ByteString.rmd160(): ByteString {
    try {
        val digest = MessageDigest.getInstance("RIPEMD160").digest(sha256().toByteArray())
        return ByteString.of(digest, 0, digest.size)
    } catch (e: NoSuchAlgorithmException) {
        throw AssertionError(e)
    }

}