package io.gnosis.safe.ui.settings.owner.ledger.transport

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Helper for serialization primitives
 */
object SerializeHelper {
    /**
     * Read an ASCII encoded string from a binary content
     * @param buffer ASCII encoded string
     * @param offset offset to the string
     * @param length length of the string
     * @return decoded string
     */
    fun readString(buffer: ByteArray?, offset: Int, length: Int): String {
        return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(Arrays.copyOfRange(buffer, offset, offset + length))).toString()
    }

    /**
     * Write a big endian encoded uint16 value into a buffer
     * @param buffer buffer to write to
     * @param value value to write
     */
    fun writeUint16BE(buffer: ByteArrayOutputStream, value: Long) {
        buffer.write((value shr 8 and 0xff).toInt())
        buffer.write((value and 0xff).toInt())
    }

    /**
     * Write a big endian encoded uint32 value into a buffer
     * @param buffer buffer to write to
     * @param value value to write
     */
    fun writeUint32BE(buffer: ByteArrayOutputStream, value: Long) {
        buffer.write((value shr 24 and 0xff).toInt())
        buffer.write((value shr 16 and 0xff).toInt())
        buffer.write((value shr 8 and 0xff).toInt())
        buffer.write((value and 0xff).toInt())
    }

    /**
     * Write a little endian encoded uint32 value into a buffer
     * @param buffer buffer to write to
     * @param value value to write
     */
    fun writeUint32LE(buffer: ByteArrayOutputStream, value: Long) {
        buffer.write((value and 0xff).toInt())
        buffer.write((value shr 8 and 0xff).toInt())
        buffer.write((value shr 16 and 0xff).toInt())
        buffer.write((value shr 24 and 0xff).toInt())
    }

    /**
     * Write a little endian encoded uint64 value into a buffer
     * @param buffer buffer to write to
     * @param value value to write
     */
    fun writeUint64LE(buffer: ByteArrayOutputStream, value: Long) {
        buffer.write((value and 0xff).toInt())
        buffer.write((value shr 8 and 0xff).toInt())
        buffer.write((value shr 16 and 0xff).toInt())
        buffer.write((value shr 24 and 0xff).toInt())
        buffer.write((value shr 32 and 0xff).toInt())
        buffer.write((value shr 40 and 0xff).toInt())
        buffer.write((value shr 48 and 0xff).toInt())
        buffer.write((value shr 56 and 0xff).toInt())
    }

    /**
     * Write a big endian encoded uint64 value into a buffer
     * @param buffer buffer to write to
     * @param value value to write
     */
    fun writeUint64BE(buffer: ByteArrayOutputStream, value: Long) {
        buffer.write((value shr 56 and 0xff).toInt())
        buffer.write((value shr 48 and 0xff).toInt())
        buffer.write((value shr 40 and 0xff).toInt())
        buffer.write((value shr 32 and 0xff).toInt())
        buffer.write((value shr 24 and 0xff).toInt())
        buffer.write((value shr 16 and 0xff).toInt())
        buffer.write((value shr 8 and 0xff).toInt())
        buffer.write((value and 0xff).toInt())
    }

    /**
     * Write a byte array value into a buffer
     * @param buffer buffer to write to
     * @param value value to write
     */
    fun writeBuffer(buffer: ByteArrayOutputStream, value: ByteArray) {
        buffer.write(value, 0, value.size)
    }

    /**
     * Convert an ASCII string into a byte array
     * @param data String to convert
     * @return encoded string
     */
    fun stringToByteArray(data: String?): ByteArray {
        return StandardCharsets.US_ASCII.encode(data).array()
    }
}
