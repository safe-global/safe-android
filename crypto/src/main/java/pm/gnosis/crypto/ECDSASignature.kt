package pm.gnosis.crypto

import pm.gnosis.crypto.utils.Curves
import java.math.BigInteger


/**
 * Groups the two components that make up a signature, and provides a way to encode to Base64 form, which is
 * how ECDSA signatures are represented when embedded in other data structures in the Ethereum protocol. The raw
 * components can be useful for doing further EC maths on them.
 */
class ECDSASignature(
        /** The two components of the signature.  */
        val r: BigInteger, val s: BigInteger
) {
    var v: Byte = 0

    /**
     * Will automatically adjust the S component to be less than or equal to half the curve order, if necessary.
     * This is required because for every signature (r,s) the signature (r, -s (mod N)) is a valid signature of
     * the same message. However, we dislike the ability to modify the bits of a Ethereum transaction after it's
     * been signed, as that violates various assumed invariants. Thus in future only one of those forms will be
     * considered legal and the other will be banned.
     */
    fun toCanonicalised(): ECDSASignature {
        return if (s > Curves.SECP256K1_HALF_CURVE_ORDER) {
            // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
            // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
            //    N = 10
            //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
            //    10 - 8 == 2, giving us always the latter solution, which is canonical.
            ECDSASignature(r, Curves.SECP256K1.n.subtract(s))
        } else {
            this
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val signature = other as ECDSASignature?

        if (r != signature!!.r) return false
        return s == signature.s

    }

    override fun hashCode(): Int {
        var result = r.hashCode()
        result = 31 * result + s.hashCode()
        return result
    }

    companion object {

        private fun fromComponents(r: ByteArray, s: ByteArray): ECDSASignature {
            return ECDSASignature(BigInteger(1, r), BigInteger(1, s))
        }

        fun fromComponents(r: ByteArray, s: ByteArray, v: Byte): ECDSASignature {
            val signature = fromComponents(r, s)
            signature.v = v
            return signature
        }
    }
}