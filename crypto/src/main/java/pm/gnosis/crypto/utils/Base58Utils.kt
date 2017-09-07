package pm.gnosis.crypto.utils

import okio.ByteString
import java.math.BigInteger


object Base58Utils {

    fun encode(source: ByteString): String {
        val sourceSize = source.size()
        if (sourceSize == 0) {
            return ""
        }

        var intData: BigInteger

        try {
            intData = BigInteger(1, source.toByteArray())
        } catch (e: NumberFormatException) {
            return ""
        }


        val result = StringBuilder()

        while (intData.compareTo(BigInteger.ZERO) == 1) {
            val quotientAndRemainder = intData.divideAndRemainder(BASE)

            val quotient = quotientAndRemainder[0]
            val remainder = quotientAndRemainder[1]

            intData = quotient

            result.append(ALPHABET[remainder.toInt()])
        }

        var i = 0
        while (i < sourceSize && source.getByte(i) == ZERO_BYTE) {
            result.append(LEADER)
            i++
        }

        return result.reverse().toString()
    }

    private fun addChecksum(data: ByteString): ByteString {
        val checksum = data.sha256().sha256()

        val resultSize = data.size() + 4
        val result = ByteArray(resultSize)

        System.arraycopy(data.toByteArray(), 0, result, 0, data.size())
        System.arraycopy(checksum.toByteArray(), 0, result, data.size(), 4)

        return ByteString.of(result, 0, resultSize)
    }

    fun encodeChecked(data: ByteString): String {
        return encode(addChecksum(data))
    }

    const val ZERO_BYTE = 0.toByte()
    const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    const val LEADER = ALPHABET.get(0)

    val BASE = BigInteger.valueOf(ALPHABET.length.toLong())
    val ALPHABET_MAP = ALPHABET.associateBy { ALPHABET.indexOf(it) }
}