package io.gnosis.safe.ui.settings.owner.ledger.transport

import io.gnosis.safe.ui.settings.owner.ledger.LedgerWrapper
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import java.io.ByteArrayOutputStream

class LedgerWrapperTest {

    @Test
    fun commandGetAddress() {
        assertEquals(
            COMMAND_GET_ADDRESS,
            LedgerWrapper.commandGetAddress(DERIVATION_PATH).toHexString()
        )
    }

    @Test
    fun commandSignMessage() {
        val txHash = "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67"
        val signMessageCommand = LedgerWrapper.commandSignMessage(DERIVATION_PATH, txHash)
        assertEquals(
            COMMAND_SIGN_MESSAGE,
            signMessageCommand.toHexString()
        )
    }

    @Test
    fun wrapAPDU() {
        assertEquals(
            COMMAND_GET_ADDRESS_WRAPPED,
            LedgerWrapper.wrapAPDU(COMMAND_GET_ADDRESS.hexToByteArray()).toHexString()
        )
    }

    @Test
    fun unwrapAPDU() {
        assertEquals(
            COMMAND_GET_ADDRESS,
            LedgerWrapper.unwrapAPDU(COMMAND_GET_ADDRESS_WRAPPED.hexToByteArray()).toHexString()
        )
    }

    @Test
    fun chunkDataAPDU() {
        val chunks = LedgerWrapper.chunkDataAPDU(COMMAND_SIGN_MESSAGE.hexToByteArray(), 20)
        assertEquals(4, chunks.size)
        val command = ByteArrayOutputStream()
        chunks.forEachIndexed { index, chunk ->
            assert(chunk.first() == LedgerWrapper.TAG_APDU.toByte())
            command.write(chunk.slice((if (index == 0) 5 else 3) until chunk.size).toByteArray())
        }
        assertEquals(
            COMMAND_SIGN_MESSAGE,
            command.toByteArray().toHexString()
        )
    }

    companion object {
        const val DERIVATION_PATH = "44'/60'/0'/14"
        const val COMMAND_GET_ADDRESS = "e002000011048000002c8000003c800000000000000e"
        const val COMMAND_GET_ADDRESS_WRAPPED = "0500000016e002000011048000002c8000003c800000000000000e"
        const val COMMAND_SIGN_MESSAGE = "e008000035048000002c8000003c800000000000000e00000020b3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67"
    }
}
