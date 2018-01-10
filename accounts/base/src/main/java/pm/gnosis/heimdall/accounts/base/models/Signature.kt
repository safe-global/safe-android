package pm.gnosis.heimdall.accounts.base.models

import java.math.BigInteger


data class Signature(val r: BigInteger, val s: BigInteger, val v: Byte) {
    override fun toString(): String {
        return r.toString(16).padStart(64, '0').substring(0, 64) +
                s.toString(16).padStart(64, '0').substring(0, 64) +
                v.toString(16).padStart(2, '0')
    }

    companion object {
        fun from(encoded: String): Signature {
            if (encoded.length != 130) throw IllegalArgumentException()
            val r = BigInteger(encoded.substring(0, 64), 16)
            val s = BigInteger(encoded.substring(64, 128), 16)
            val v = encoded.substring(128, 130).toByte(16)
            return Signature(r, s, v)
        }
    }
}