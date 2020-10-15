package io.gnosis.data.utils

import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.dto.GateTransactionDetailsDto
import io.gnosis.data.models.DetailedExecutionInfo
import io.gnosis.data.repositories.TransactionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class SafeTxHashTest {

    private val gatewayApi = mockk<GatewayApi>()
    private val transactionRepository = TransactionRepository(gatewayApi)

    private val moshi = dataMoshi
    private val txDtoAdapter = moshi.adapter(GateTransactionDetailsDto::class.java)

    private val safeAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!

    @Test
    fun `calculateSafeTxHash (safe, customTx) should return same value as in customTx`() = runBlocking {

        val json = readResource("tx_details_custom.json")
        val txCustomDto = txDtoAdapter.fromJson(json)
        coEvery { gatewayApi.loadTransactionDetails(any()) } returns txCustomDto!!
        val txCustom = transactionRepository.getTransactionDetails("id")

        val txCustomSafeTxHash = (txCustom.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails).safeTxHash
        val calculatedTxHash = calculateSafeTxHash(safeAddress, txCustom)?.toHexString()?.addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    @Test
    fun `calculateSafeTxHash (safe, transferTx) should return same value as in transferTx`() = runBlocking {

        val json = readResource("tx_details_transfer.json")
        val txTransferDto = txDtoAdapter.fromJson(json)
        coEvery { gatewayApi.loadTransactionDetails(any()) } returns txTransferDto!!
        val txTransfer = transactionRepository.getTransactionDetails("id")

        val txCustomSafeTxHash = (txTransfer.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails).safeTxHash
        val calculatedTxHash = calculateSafeTxHash(safeAddress, txTransfer)?.toHexString()?.addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    @Test
    fun `calculateSafeTxHash (safe, settingsChangeTx) should return same value as in settingsChangeTx`() = runBlocking {

        val json = readResource("tx_details_settings_change.json")
        val txSettingsChangeDto = txDtoAdapter.fromJson(json)
        coEvery { gatewayApi.loadTransactionDetails(any()) } returns txSettingsChangeDto!!
        val txSettingsChange = transactionRepository.getTransactionDetails("id")

        val txCustomSafeTxHash = (txSettingsChange.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails).safeTxHash
        val calculatedTxHash = calculateSafeTxHash(safeAddress, txSettingsChange)?.toHexString()?.addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    private fun readResource(fileName: String): String {
        return BufferedReader(
            InputStreamReader(
                this::class.java.getClassLoader()?.getResourceAsStream(fileName)!!
            )
        ).lines().parallel().collect(Collectors.joining("\n"))
    }
}
