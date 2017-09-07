package pm.gnosis.crypto.utils

import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.crypto.params.ECDomainParameters
import java.math.BigInteger


object Curves {

    /** The parameters of the secp256k1 curve that Ethereum uses.  */
    val SECP256K1: ECDomainParameters

    /**
     * Equal to CURVE.getN().shiftRight(1), used for canonicalising the S value of a signature. If you aren't
     * sure what this is about, you can ignore it.
     */
    val SECP256K1_HALF_CURVE_ORDER: BigInteger

    init {
        // All clients must agree on the curve to use by agreement. Ethereum uses secp256k1.
        val params = SECNamedCurves.getByName("secp256k1")
        SECP256K1 = ECDomainParameters(params.curve, params.g, params.n, params.h)
        SECP256K1_HALF_CURVE_ORDER = params.n.shiftRight(1)
    }
}