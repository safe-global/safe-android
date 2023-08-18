package io.gnosis.data.utils

import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.utils.removeHexPrefix

fun ECDSASignature.toSignatureString() =
    r.toString(16).padStart(64, '0').substring(0, 64) +
            s.toString(16).padStart(64, '0').substring(0, 64) +
            v.toString(16).padStart(2, '0')


fun String.toSignature(): ECDSASignature {
    val signatureString = this.removeHexPrefix()
    val r = signatureString.substring(0, 64).toBigInteger(16).toByteArray()
    val s = signatureString.substring(64, 128).toBigInteger(16).toByteArray()
    val v = signatureString.substring(128, 130).toBigInteger(16).toByte()
    return ECDSASignature.fromComponents(r, s, v)
}
