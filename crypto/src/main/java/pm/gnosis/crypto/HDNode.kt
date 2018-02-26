package pm.gnosis.crypto

import okio.Buffer
import okio.ByteString
import pm.gnosis.crypto.utils.Base58Utils
import pm.gnosis.crypto.utils.Curves
import pm.gnosis.crypto.utils.hash160
import pm.gnosis.utils.bigInt
import pm.gnosis.utils.toBytes
import java.math.BigInteger


class HDNode(val keyPair: KeyPair, val chainCode: ByteString, val depth: Int, val index: Long, val parentFingerprint: ByteString) {
    fun derive(path: String): HDNode {
        if (path == "m" || path == "M" || path == "m'" || path == "M'") {
            return this
        }

        val parts = path.split("/")

        var key = this
        parts.forEachIndexed { index, s ->
            if (index == 0) {
                if (s != "m") {
                    throw IllegalArgumentException("Invalid path")
                } else {
                    return@forEachIndexed
                }
            }

            val hardened = s.length > 1 && s[s.length - 1] == '\''
            val cleanS = s.replace("'", "")
            var childIndex = cleanS.toLong()

            if (childIndex >= HARDENED_OFFSET) {
                throw IllegalArgumentException("Invalid index")
            }
            if (hardened) {
                childIndex += HARDENED_OFFSET
            }

            key = key.deriveChild(childIndex)
        }
        return key
    }

    fun deriveChild(index: Long): HDNode {
        val isHardened = index >= HARDENED_OFFSET
        val dataBuffer = Buffer()
        if (isHardened) {
            dataBuffer.writeByte(0)
            dataBuffer.write(keyPair.privKeyBytes)
        } else {
            dataBuffer.write(publicKey()) // Use public key
        }
        dataBuffer.writeInt(index.toInt())
        val bytes = dataBuffer.hmacSha512(chainCode)

        return try {
            val newChainCode = bytes.substring(32)
            HDNode(generateKeyPair(bytes.substring(0, 32).bigInt(), keyPair.privKey), newChainCode, depth + 1, index, fingerprint().substring(0, 4))
        } catch (e: Exception) {
            deriveChild(index + 1)
        }
    }

    private fun generateKeyPair(pkn: BigInteger, ccn: BigInteger): KeyPair {
        if (pkn.signum() <= 0) {
            throw IllegalArgumentException("Private key must be greater than 0")
        }
        if (pkn >= Curves.SECP256K1.n) {
            throw IllegalArgumentException("Private key must be less than the curve order")
        }

        val an = pkn.add(ccn).mod(Curves.SECP256K1.n)
        if (an.signum() <= 0) {
            throw IllegalArgumentException("Private key must be greater than 0")
        }
        val adjusted = an.toBytes(32)
        return KeyPair.fromPrivate(adjusted)
    }

    fun toBase58(): String {
        return Base58Utils.encodeChecked(
            Buffer()
                // var network = this.keyPair.network
                // var version = (!this.isNeutered()) ? network.bip32.private : network.bip32.public
                // 4 bytes: version bytes
                .writeInt(VERSION)
                // 1 byte: depth: 0x00 for master nodes, 0x01 for level-1 descendants, ....
                .writeByte(depth)
                // 4 bytes: the parentFingerprint of the parent's key (0x00000000 if master key)
                .write(parentFingerprint)
                // 4 bytes: child number. This is the number i in xi = xpar/i, with xi the key being serialized.
                .writeInt(index.toInt())
                // 32 bytes: the chain code
                .write(chainCode)
                // 33 bytes: private key data (0 + 32 key)
                .writeByte(0)
                .write(keyPair.privKeyBytes)
                .readByteString()
        )
    }

    fun publicKey(): ByteString {
        val encoded = keyPair.pubKey
        return ByteString.of(encoded, 0, encoded.size)
    }

    fun identifier(): ByteString {
        return publicKey().hash160()
    }

    fun fingerprint(): ByteString {
        return identifier().substring(0, 4)
    }

    companion object {
        private const val VERSION = 0x0488ade4
        private const val HARDENED_OFFSET = 0x80000000
    }
}
