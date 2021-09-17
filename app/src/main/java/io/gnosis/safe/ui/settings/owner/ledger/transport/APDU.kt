package io.gnosis.safe.ui.settings.owner.ledger

import io.gnosis.safe.ui.settings.owner.ledger.transport.LedgerException
import io.gnosis.safe.ui.settings.owner.ledger.transport.SerializeHelper
import java.io.ByteArrayOutputStream


object LedgerWrapper {

    private const val TAG_APDU = 0x05

    fun wrapADPU(data: ByteArray): ByteArray {
        val apdu = ByteArrayOutputStream()
        apdu.write(TAG_APDU)
        apdu.write(0x00)
        apdu.write(0x00)
        SerializeHelper.writeUint16BE(apdu, data.count().toLong())
        apdu.write(data)
        return apdu.toByteArray()
    }

    fun unwrapADPU(data: ByteArray): ByteArray {
        if (data.size <= 6 || data[0] != 0x05.toByte()) {
            throw LedgerException(LedgerException.ExceptionReason.IO_ERROR, "invalid data size")
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

        val address = data.slice(publicKeyLength + 2..publicKeyLength + addressLength).toByteArray()
        return address.toString(Charsets.US_ASCII)
    }
}









