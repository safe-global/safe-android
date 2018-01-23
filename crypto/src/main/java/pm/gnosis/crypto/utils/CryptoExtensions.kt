package pm.gnosis.crypto.utils

import okio.ByteString
import org.spongycastle.crypto.digests.RIPEMD160Digest
import org.spongycastle.jcajce.provider.digest.RIPEMD160
import java.lang.AssertionError
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


fun ByteString.rmd160(): ByteString {
    try {
        val digest = RIPEMD160.Digest().digest(sha256().toByteArray())
        return ByteString.of(digest, 0, digest.size)
    } catch (e: NoSuchAlgorithmException) {
        throw AssertionError(e)
    }

}