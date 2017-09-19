package pm.gnosis.crypto

import okio.ByteString
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException


class KeyGenerator {
    /*
    Test transaction that can be used.
    Signed transaction hash is (when used with TestRPC account 0):
    f86c098504a817c800825208943535353535353535353535353535353535353535880de0b6b3a76400008026a089b20bc88a68ccfffab9a0461e05bafbeecd5d1b2f8e4aa611657c5befde6e6aa0633dcedb94d140e3a88389c37d2a9bbfd46a890976524fafb67ccc04bff237a3
    val transaction = Transaction(
                            nonce = BigInteger("9"),
                            gasPrice = BigInteger("20000000000"),
                            startGas = BigInteger("21000"),
                            to = "3535353535353535353535353535353535353535".hexAsBigInteger(),
                            value = BigInteger("1000000000000000000"),
                            data = ByteArray(0),
                            chainCode = 1
                    )
     */

    @Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun masterNode(seed: ByteString): HDNode {
        val hash = seed.hmacSha512(ByteString.encodeUtf8(MASTER_SECRET))
        return HDNode(KeyPair.fromPrivate(hash.substring(0, 32).toByteArray()), hash.substring(32), 0, 0, ByteString.of(0, 0, 0, 0))
    }

    companion object {
        const val BIP44_PATH_ETHEREUM = "m/44'/60'/0'/0"

        const val MASTER_SECRET = "Bitcoin seed"
    }

}
