package io.gnosis.safe.ui.settings.owner.ledger

import io.gnosis.safe.ui.settings.owner.ledger.transport.LedgerException
import io.gnosis.safe.ui.settings.owner.ledger.transport.SerializeHelper
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.io.ByteArrayOutputStream


object LedgerWrapper {

    const val TAG_APDU = 0x05

    fun chunkDataAPDU(data: ByteArray, chunkSize: Int): List<ByteArray> {
        var chunkPayloadStartIndex = 0
        var chunkPayloadEndIndex = 0
        var chunk = 0
        val chunks = mutableListOf<ByteArray>()
        while (chunkPayloadEndIndex < data.size) {
            chunkPayloadEndIndex = if (chunkPayloadStartIndex + chunkSize >= data.size) data.size else chunkPayloadStartIndex + chunkSize
            if (chunk == 0) {
                chunkPayloadEndIndex -= 5
            } else if (chunkPayloadEndIndex - chunkPayloadStartIndex == chunkSize) {
                chunkPayloadEndIndex -= 3
            }
            val chunkBytes = ByteArrayOutputStream()
            chunkBytes.write(TAG_APDU)
            SerializeHelper.writeUint16BE(chunkBytes, chunk.toLong())
            if (chunk == 0) {
                SerializeHelper.writeUint16BE(chunkBytes, data.count().toLong())
            }
            chunkBytes.write(data, chunkPayloadStartIndex, chunkPayloadEndIndex - chunkPayloadStartIndex)

            chunks.add(chunkBytes.toByteArray())
            chunk += 1
            chunkPayloadStartIndex = chunkPayloadEndIndex
        }
        return chunks
    }

    fun wrapAPDU(data: ByteArray): ByteArray {
        val apdu = ByteArrayOutputStream()
        apdu.write(TAG_APDU)
        apdu.write(0x00)
        apdu.write(0x00)
        SerializeHelper.writeUint16BE(apdu, data.count().toLong())
        apdu.write(data)
        return apdu.toByteArray()
    }

    fun unwrapAPDU(data: ByteArray): ByteArray {
        if (data.size <= 6 || data[0] != 0x05.toByte()) {
            throw LedgerException(LedgerException.ExceptionReason.IO_ERROR, "invalid data format")
        }
        val unwrappedData = data.slice(5..data.size - 1)
        return unwrappedData.toByteArray()
    }

    fun splitPath(path: String): ByteArray {
        if (path.isEmpty()) {
            return byteArrayOf(0)
        }
        val elements = path.split("/").toTypedArray()
        if (elements.size > 10) {
            throw LedgerException(LedgerException.ExceptionReason.INTERNAL_ERROR, "Path too long")
        }
        val result = ByteArrayOutputStream()
        result.write(elements.size)
        for (element in elements) {
            var elementValue: Long
            val hardenedIndex = element.indexOf('\'')
            if (hardenedIndex > 0) {
                elementValue = element.substring(0, hardenedIndex).toLong()
                elementValue = elementValue or -0x80000000
            } else {
                elementValue = element.toLong()
            }
            SerializeHelper.writeUint32BE(result, elementValue)
        }
        return result.toByteArray()
    }

    fun parseGetAddress(data: ByteArray): String {
        if (data.size != 109) throw LedgerException(LedgerException.ExceptionReason.IO_ERROR, "invalid data size")
        if (data[0] != 65.toByte()) throw LedgerException(LedgerException.ExceptionReason.IO_ERROR, "invalid public key length")
        if (data[66] != 40.toByte()) throw LedgerException(LedgerException.ExceptionReason.IO_ERROR, "invalid address length")

        val publicKeyLength = 65
        val addressLength = 40

        val address = data.slice(publicKeyLength + 2..publicKeyLength + addressLength + 1).toByteArray()
        return address.toString(Charsets.US_ASCII)
    }

    fun parseSignMessage(data: ByteArray): String {
        if (data.size < 65) throw LedgerException(LedgerException.ExceptionReason.INVALID_PARAMETER, "invalid data size")
        
        val v = data[0] + 4.toByte()
        val r = data.slice(1..32).toByteArray().asBigInteger()
        val s = data.slice(33..64).toByteArray().asBigInteger()

        return "0x" + r.toString(16).padStart(64, '0').substring(0, 64) +
                s.toString(16).padStart(64, '0').substring(0, 64) +
                v.toString(16).padStart(2, '0')
    }

    fun commandSignTx(path: String, encodedTx: String): ByteArray {

        val pathsData = splitPath(path)
        val txBytes = encodedTx.hexToByteArray()

        val commandData = mutableListOf<Byte>()
        commandData.add(0xe0.toByte())
        commandData.add(0x04.toByte())
        commandData.add(0x00.toByte())
        commandData.add(0x00.toByte())

        val txData = ByteArrayOutputStream()
        SerializeHelper.writeUint32BE(txData, txBytes.size.toLong())
        txBytes.forEachIndexed { index, element ->
            txData.write(element.toInt())
        }

        commandData.add((pathsData.size + txBytes.size + 4).toByte())
        commandData.addAll(pathsData.toList())
        commandData.addAll(txData.toByteArray().toList())

        val command = commandData.toByteArray()
        Timber.d("Sign tx command: ${command.toHexString()}")

        return command
    }

    fun commandSignMessage(path: String, message: String): ByteArray {

        val pathsData = splitPath(path)
        val messageBytes = message.hexToByteArray()

        val commandData = mutableListOf<Byte>()
        commandData.add(0xe0.toByte())
        commandData.add(0x08.toByte())
        commandData.add(0x00.toByte())
        commandData.add(0x00.toByte())

        val messageData = ByteArrayOutputStream()
        SerializeHelper.writeUint32BE(messageData, messageBytes.size.toLong())
        messageBytes.forEachIndexed { index, element ->
            messageData.write(element.toInt())
        }

        commandData.add((pathsData.size + messageBytes.size + 4).toByte())
        commandData.addAll(pathsData.toList())
        commandData.addAll(messageData.toByteArray().toList())

        // Command length should be 150 bytes length otherwise we should split
        // it into chuncks. As we sign hashes we should be fine for now.
        val command = commandData.toByteArray()
        Timber.d("Sign command: ${command.toHexString()}")

        if (command.size > 150) throw LedgerException(LedgerException.ExceptionReason.IO_ERROR, "invalid data format")

        return command
    }

    fun commandGetAddress(path: String, displayVerificationDialog: Boolean = false, chainCode: Boolean = false): ByteArray {

        val paths = splitPath(path)

        val commandData = mutableListOf<Byte>()

        val pathsData = ByteArray(1 + paths.size)
        pathsData[0] = paths.size.toByte()

        paths.forEachIndexed { index, element ->
            pathsData[1 + index] = element
        }

        commandData.add(0xe0.toByte())
        commandData.add(0x02.toByte())
        commandData.add((if (displayVerificationDialog) 0x01.toByte() else 0x00.toByte()))
        commandData.add((if (chainCode) 0x01.toByte() else 0x00.toByte()))
        commandData.addAll(pathsData.toList())

        val command = commandData.toByteArray()
        Timber.d("Get address command: ${command.toHexString()}")

        return command
    }
}
