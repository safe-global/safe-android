package pm.gnosis.heimdall.accounts.base.models

import java.math.BigInteger


data class Signature(val r: BigInteger, val s: BigInteger, val v: Byte) {
    override fun toString(): String {
        return r.toString(16).padStart(64, '0') +
                s.toString(16).padStart(64, '0') +
                v.toString(16).padStart(2, '0')

    }
}