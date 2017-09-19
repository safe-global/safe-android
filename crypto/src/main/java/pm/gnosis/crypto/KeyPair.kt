@file:Suppress("unused")

package pm.gnosis.crypto

/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.spongycastle.asn1.x9.X9IntegerConverter
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.params.ECPrivateKeyParameters
import org.spongycastle.crypto.params.ECPublicKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.crypto.signers.HMacDSAKCalculator
import org.spongycastle.math.ec.ECAlgorithms
import org.spongycastle.math.ec.ECCurve
import org.spongycastle.math.ec.ECPoint
import org.spongycastle.util.encoders.Base64
import org.spongycastle.util.encoders.Hex
import pm.gnosis.crypto.exceptions.MissingPrivateKeyException
import pm.gnosis.crypto.utils.Curves.SECP256K1
import pm.gnosis.crypto.utils.HashUtils
import pm.gnosis.utils.toBytes
import pm.gnosis.utils.toHexString
import java.io.Serializable
import java.math.BigInteger
import java.security.SignatureException
import java.util.*

/**
 *
 * Represents an elliptic curve public and (optionally) private key, usable for digital signatures but not encryption.
 * Creating a new ECKey with the empty constructor will generate a new random keypair. Other static methods can be used
 * when you already have the public or private parts. If you create a key with only the public part, you can check
 * signatures but not create them.
 *
 *
 * The ECDSA algorithm supports *key recovery* in which a signature plus a couple of discriminator bits can
 * be reversed to find the public key used to calculate it. This can be convenient when you have a message and a
 * signature and want to find out who signed it, rather than requiring the user to provide the expected identity.
 *
 *
 * A key can be *compressed* or *uncompressed*. This refers to whether the public key is represented
 * when encoded into bytes as an (x, y) coordinate on the elliptic curve, or whether it's represented as just an X
 * co-ordinate and an extra byte that carries a sign bit. With the latter form the Y coordinate can be calculated
 * dynamically, however, **because the binary serialization is different the address of a key changes if its
 * compression status is changed**. If you deviate from the defaults it's important to understand this: money sent
 * to a compressed version of the key will have a different address to the same key in uncompressed form. Whether
 * a public key is compressed or not is recorded in the SEC binary serialisation format, and preserved in a flag in
 * this class so round-tripping preserves state. Unless you're working with old software or doing unusual things, you
 * can usually ignore the compressed/uncompressed distinction.
 *
 * This code is borrowed from the bitcoinj project and altered to fit Ethereum.<br></br>
 * See [bitcoinj on GitHub](https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/com/google/bitcoin/core/ECKey.java)
 *
 * This is an adjusted version of the ethereumj class, tranformed to kotlin.<br></br>
 * See [ECKey on GitHub](https://raw.githubusercontent.com/ethereumj/ethereumj/master/ethereumj-core/src/main/java/org/ethereum/crypto/ECKey.java)
 */
class KeyPair(
        // The two parts of the key. If "priv" is set, "pub" can always be calculated. If "pub" is set but not "priv", we
        // can only verify signatures not make them.
        private val priv: BigInteger?,
        private val pubKeyPoint: ECPoint
) : Serializable {

    private val pubKeyHash by lazy {
        val pubBytes = pubKeyPoint.getEncoded(false)
        HashUtils.sha3lower20(Arrays.copyOfRange(pubBytes, 1, pubBytes.size))
    }

    /**
     * Returns a copy of this key, but with the public point represented in uncompressed form. Normally you would
     * never need this: it's for specialised scenarios or when backwards compatibility in encoded form is necessary.
     */
    fun decompress(): KeyPair {
        return if (!pubKeyPoint.isCompressed)
            this
        else
            KeyPair(priv, decompressPoint(pubKeyPoint))
    }

    /**
     * Returns true if this key doesn't have access to private key bytes. This may be because it was never
     * given any private key bytes to begin with (a watching key).
     */
    val isPubKeyOnly: Boolean
        get() = priv == null

    /**
     * Returns true if this key has access to private key bytes. Does the opposite of
     * [.isPubKeyOnly].
     */
    fun hasPrivKey(): Boolean {
        return priv != null
    }

    /** Gets the hash160 form of the public key (as seen in addresses).  */
    val address: ByteArray
        get() {
            return pubKeyHash
        }

    /**
     * Gets the raw public key value. This appears in transaction scriptSigs. Note that this is **not** the same
     * as the pubKeyHash/address.
     */
    val pubKey: ByteArray
        get() = pubKeyPoint.encoded

    /**
     * Gets the private key in the form of an integer field element. The public key is derived by performing EC
     * point addition this number of times (i.e. point multiplying).
     *
     * @throws java.lang.IllegalStateException if the private key bytes are not available.
     */
    val privKey: BigInteger
        get() {
            if (priv == null)
                throw MissingPrivateKeyException()
            return priv
        }

    /**
     * Returns whether this key is using the compressed form or not. Compressed pubkeys are only 33 bytes, not 64.
     */
    val isCompressed: Boolean
        get() = pubKeyPoint.isCompressed

    override fun toString(): String {
        val b = StringBuilder()
        b.append("pub:").append(Hex.toHexString(pubKeyPoint.getEncoded(false)))
        return b.toString()
    }

    /**
     * Produce a string rendering of the ECKey INCLUDING the private key.
     * Unless you absolutely need the private key it is better for security reasons to just use toString().
     */
    fun toStringWithPrivate(): String {
        val b = StringBuilder()
        b.append(toString())
        if (priv != null) {
            b.append(" priv:").append(Hex.toHexString(priv.toByteArray()))
        }
        return b.toString()
    }

    /**
     * Signs the given hash and returns the R and S components as BigIntegers
     * and put them in ECDSASignature
     *
     * @param input to sign
     * @return ECDSASignature signature that contains the R and S components
     */
    fun doSign(input: ByteArray): ECDSASignature {
        // No decryption of private key required.
        if (priv == null)
            throw MissingPrivateKeyException()
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        val privKey = ECPrivateKeyParameters(priv, SECP256K1)
        signer.init(true, privKey)
        val components = signer.generateSignature(input)
        return ECDSASignature(components[0], components[1]).toCanonicalised()
    }

    /**
     * Takes the sha3 hash (32 bytes) of data and returns the ECDSA signature
     *
     * @throws IllegalStateException if this ECKey does not have the private part.
     */
    fun sign(messageHash: ByteArray): ECDSASignature {
        if (priv == null)
            throw MissingPrivateKeyException()
        val sig = doSign(messageHash)
        // Now we have to work backwards to figure out the recId needed to recover the signature.
        var recId = -1
        for (i in 0..3) {
            val k = recoverFromSignature(i, sig, messageHash, isCompressed)
            if (k != null && k.pubKeyPoint.equals(pubKeyPoint)) {
                recId = i
                break
            }
        }
        if (recId == -1)
            throw RuntimeException("Could not construct a recoverable key. This should never happen.")
        sig.v = (recId + 27).toByte()
        return sig
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
     *
     * @param data      Hash of the data to verify.
     * @param signature signature.
     */
    fun verify(data: ByteArray, signature: ByteArray): Boolean {
        return verify(data, signature, pubKey)
    }

    /**
     * Verifies the given R/S pair (signature) against a hash using the public key.
     */
    fun verify(sigHash: ByteArray, signature: ECDSASignature): Boolean {
        return verify(sigHash, signature, pubKey)
    }

    /**
     * Returns true if this pubkey is canonical, i.e. the correct length taking into account compression.
     */
    val isPubKeyCanonical: Boolean
        get() = isPubKeyCanonical(pubKeyPoint.encoded)

    /**
     * Returns a 32 byte array containing the private key, or null if the key is encrypted or public only
     */
    val privKeyBytes: ByteArray?
        get() = priv?.toBytes(32)

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || o !is KeyPair) return false

        val ecKey = o as KeyPair?

        if (priv != null && priv != ecKey!!.priv) return false
        return pubKeyPoint.equals(ecKey!!.pubKeyPoint)

    }

    override fun hashCode(): Int {
        // Public keys are random already so we can just use a part of them as the hashcode. Read from the start to
        // avoid picking up the type code (compressed vs uncompressed) which is tacked on the end.
        val bits = pubKey
        return maskAndShift(bits[0], 0) or maskAndShift(bits[1], 8) or maskAndShift(bits[2], 16) or maskAndShift(bits[3], 24)
    }

    private fun maskAndShift(byte: Byte, shift: Int): Int {
        return (byte.toInt() and 0xFF) shl shift
    }

    companion object {

        /**
         * Utility for compressing an elliptic curve point. Returns the same point if it's already compressed.
         * See the ECKey class docs for a discussion of point compression.
         */
        fun compressPoint(uncompressed: ECPoint): ECPoint {
            return SECP256K1.curve.decodePoint(uncompressed.getEncoded(true))
        }

        /**
         * Utility for decompressing an elliptic curve point. Returns the same point if it's already compressed.
         * See the ECKey class docs for a discussion of point compression.
         */
        fun decompressPoint(compressed: ECPoint): ECPoint {
            return SECP256K1.curve.decodePoint(compressed.getEncoded(false))
        }

        /**
         * Creates an ECKey given the private key only.  The public key is calculated from it (this is slow). Note that
         * the resulting public key is compressed.
         */
        fun fromPrivate(privKey: BigInteger): KeyPair {
            return KeyPair(privKey, compressPoint(SECP256K1.g.multiply(privKey)))
        }

        /**
         * Creates an ECKey given the private key only.  The public key is calculated from it (this is slow). The resulting
         * public key is compressed.
         */
        fun fromPrivate(privKeyBytes: ByteArray): KeyPair {
            return fromPrivate(BigInteger(1, privKeyBytes))
        }

        /**
         * Creates an ECKey that simply trusts the caller to ensure that point is really the result of multiplying the
         * generator point by the private key. This is used to speed things up when you know you have the right values
         * already. The compression state of pub will be preserved.
         */
        fun fromPrivateAndPrecalculatedPublic(priv: BigInteger, pub: ECPoint): KeyPair {
            return KeyPair(priv, pub)
        }

        /**
         * Creates an ECKey that simply trusts the caller to ensure that point is really the result of multiplying the
         * generator point by the private key. This is used to speed things up when you know you have the right values
         * already. The compression state of the point will be preserved.
         */
        fun fromPrivateAndPrecalculatedPublic(priv: ByteArray?, pub: ByteArray?): KeyPair {
            check(priv != null, "Private key must not be null")
            check(pub != null, "Public key must not be null")
            return KeyPair(BigInteger(1, priv), SECP256K1.curve.decodePoint(pub!!))
        }

        /**
         * Creates an ECKey that cannot be used for signing, only verifying signatures, from the given point. The
         * compression state of pub will be preserved.
         */
        fun fromPublicOnly(pub: ECPoint): KeyPair {
            return KeyPair(null, pub)
        }

        /**
         * Creates an ECKey that cannot be used for signing, only verifying signatures, from the given encoded point.
         * The compression state of pub will be preserved.
         */
        fun fromPublicOnly(pub: ByteArray): KeyPair {
            return KeyPair(null, SECP256K1.curve.decodePoint(pub))
        }

        /**
         * Returns public key bytes from the given private key. To convert a byte array into a BigInteger, use <tt>
         * new BigInteger(1, bytes);</tt>
         */
        fun publicKeyFromPrivate(privKey: BigInteger, compressed: Boolean): ByteArray {
            val point = SECP256K1.g.multiply(privKey)
            return point.getEncoded(compressed)
        }

        /**
         * Given a piece of text and a message signature encoded in base64, returns an ECKey
         * containing the public key that was used to sign it. This can then be compared to the expected public key to
         * determine if the signature was correct.
         *
         * @param messageHash a piece of human readable text that was signed
         * @param signatureBase64 The Ethereum-format message signature in base64
         * @throws SignatureException If the public key could not be recovered or if there was a signature format error.
         */
        @Throws(SignatureException::class)
        fun signatureToKey(messageHash: ByteArray, signatureBase64: String): KeyPair {
            val signatureEncoded: ByteArray
            try {
                signatureEncoded = Base64.decode(signatureBase64)
            } catch (e: RuntimeException) {
                // This is what you get back from Bouncy Castle if base64 doesn't decode :(
                throw SignatureException("Could not decode base64", e)
            }

            // Parse the signature bytes into r/s and the selector value.
            if (signatureEncoded.size < 65) {
                throw SignatureException("Signature truncated, expected 65 bytes and got " + signatureEncoded.size)
            }
            var header = signatureEncoded[0].toInt() and 0xFF
            // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
            //                  0x1D = second key with even y, 0x1E = second key with odd y
            if (header < 27 || header > 34)
                throw SignatureException("Header byte out of range: " + header)
            val r = BigInteger(1, Arrays.copyOfRange(signatureEncoded, 1, 33))
            val s = BigInteger(1, Arrays.copyOfRange(signatureEncoded, 33, 65))
            val sig = ECDSASignature(r, s)
            var compressed = false
            if (header >= 31) {
                compressed = true
                header -= 4
            }
            val recId = header - 27
            return recoverFromSignature(recId, sig, messageHash, compressed) ?: throw SignatureException("Could not recover public key from signature")
        }

        /**
         *
         * Verifies the given ECDSA signature against the message bytes using the public key bytes.
         *
         *
         * When using native ECDSA verification, data must be 32 bytes, and no element may be
         * larger than 520 bytes.
         *
         * @param data      Hash of the data to verify.
         * @param signature signature.
         * @param pub       The public key bytes to use.
         */
        fun verify(data: ByteArray, signature: ECDSASignature, pub: ByteArray): Boolean {
            val signer = ECDSASigner()
            val params = ECPublicKeyParameters(SECP256K1.curve.decodePoint(pub), SECP256K1)
            signer.init(false, params)
            try {
                return signer.verifySignature(data, signature.r, signature.s)
            } catch (npe: NullPointerException) {
                // Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures.
                // Those signatures are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
                return false
            }

        }

        /**
         * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
         *
         * @param data      Hash of the data to verify.
         * @param signature signature.
         * @param pub       The public key bytes to use.
         */
        fun verify(data: ByteArray, signature: ByteArray, pub: ByteArray): Boolean {
            return verify(data, signature, pub)
        }

        /**
         * Returns true if the given pubkey is canonical, i.e. the correct length taking into account compression.
         */
        fun isPubKeyCanonical(pubkey: ByteArray): Boolean {
            if (pubkey[0].toInt() == 0x04) {
                // Uncompressed pubkey
                if (pubkey.size != 65)
                    return false
            } else if (pubkey[0].toInt() == 0x02 || pubkey[0].toInt() == 0x03) {
                // Compressed pubkey
                if (pubkey.size != 33)
                    return false
            } else
                return false
            return true
        }

        /**
         *
         * Given the components of a signature and a selector value, recover and return the public key
         * that generated the signature according to the algorithm in SEC1v2 section 4.1.6.
         *
         *
         * The recId is an index from 0 to 3 which indicates which of the 4 possible keys is the correct one. Because
         * the key recovery operation yields multiple potential keys, the correct key must either be stored alongside the
         * signature, or you must be willing to try each recId in turn until you find one that outputs the key you are
         * expecting.
         *
         *
         * If this method returns null it means recovery was not possible and recId should be iterated.
         *
         *
         * Given the above two points, a correct usage of this method is inside a for loop from 0 to 3, and if the
         * output is null OR a key that is not the one you expect, you try again with the next recId.
         *
         * @param recId Which possible key to recover.
         * @param sig the R and S components of the signature, wrapped.
         * @param messageHash Hash of the data that was signed.
         * @param compressed Whether or not the original pubkey was compressed.
         * @return An ECKey containing only the public part, or null if recovery wasn't possible.
         */
        fun recoverFromSignature(recId: Int, sig: ECDSASignature, messageHash: ByteArray?, compressed: Boolean): KeyPair? {
            check(recId >= 0, "recId must be positive")
            check(sig.r.signum() >= 0, "r must be positive")
            check(sig.s.signum() >= 0, "s must be positive")
            check(messageHash != null, "messageHash must not be null")
            // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
            //   1.1 Let x = r + jn
            val n = SECP256K1.n  // Curve order.
            val i = BigInteger.valueOf(recId.toLong() / 2)
            val x = sig.r.add(i.multiply(n))
            //   1.2. Convert the integer x to an octet string X of length mlen using the conversion routine
            //        specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
            //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
            //        conversion routine specified in Section 2.3.4. If this conversion routine outputs “invalid”, then
            //        do another iteration of Step 1.
            //
            // More concisely, what these points mean is to use X as a compressed public key.
            val curve = SECP256K1.curve as ECCurve.Fp
            val prime = curve.q  // Bouncy Castle is not consistent about the letter it uses for the prime.
            if (x.compareTo(prime) >= 0) {
                // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
                return null
            }
            // Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
            // So it's encoded in the recId.
            val R = decompressKey(x, recId and 1 == 1)
            //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
            if (!R.multiply(n).isInfinity)
                return null
            //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
            val e = BigInteger(1, messageHash)
            //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
            //   1.6.1. Compute a candidate public key as:
            //               Q = mi(r) * (sR - eG)
            //
            // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
            //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
            // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n). In the above equation
            // ** is point multiplication and + is point addition (the EC group operator).
            //
            // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
            // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
            val eInv = BigInteger.ZERO.subtract(e).mod(n)
            val rInv = sig.r.modInverse(n)
            val srInv = rInv.multiply(sig.s).mod(n)
            val eInvrInv = rInv.multiply(eInv).mod(n)
            val q = ECAlgorithms.sumOfTwoMultiplies(SECP256K1.g, eInvrInv, R, srInv) as ECPoint.Fp
            return fromPublicOnly(q.getEncoded(compressed))
        }

        /** Decompress a compressed public key (x co-ord and low-bit of y-coord).  */
        private fun decompressKey(xBN: BigInteger, yBit: Boolean): ECPoint {
            val x9 = X9IntegerConverter()
            val compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(SECP256K1.curve))
            compEnc[0] = (if (yBit) 0x03 else 0x02).toByte()
            return SECP256K1.curve.decodePoint(compEnc)
        }

        private fun check(test: Boolean, message: String) {
            if (!test) throw IllegalArgumentException(message)
        }
    }
}
/**
 * Generates an entirely new keypair. Point compression is used so the resulting public key will be 33 bytes
 * (32 for the co-ordinate and 1 byte to represent the y bit).
 */