package io.gnosis.data.utils

import pm.gnosis.crypto.ECDSASignature

fun ECDSASignature.toSignatureString() =
    r.toString(16).padStart(64, '0').substring(0, 64) +
            s.toString(16).padStart(64, '0').substring(0, 64) +
            v.toString(16).padStart(2, '0')
