package io.gnosis.safe.utils

import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.readJsonFrom
import io.gnosis.safe.ui.transactions.details.toTransactionDetailsViewData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString

class SafeTxHashTest {

    private val gatewayApi = mockk<GatewayApi>()
    private val transactionRepository = TransactionRepository(gatewayApi)

    private val txDtoAdapter = dataMoshi.adapter(TransactionDetails::class.java)

    private val safeAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!

    @Test
    fun `calculateSafeTxHash (safe, customTx) should return same value as in customTx`() = runBlocking {
        val txCustomDto = txDtoAdapter.readJsonFrom("tx_details_custom.json")
        coEvery { gatewayApi.loadTransactionDetails(any()) } returns txCustomDto
        val txCustom = transactionRepository.getTransactionDetails("id")
        val executionInfo = txCustom.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails

        val txCustomSafeTxHash = executionInfo.safeTxHash
        val calculatedTxHash = calculateSafeTxHash(safeAddress, txCustom.toTransactionDetailsViewData(emptyList()), executionInfo)?.toHexString()?.addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    @Test
    fun `calculateSafeTxHash (safe, transferTx) should return same value as in transferTx`() = runBlocking {
        val txTransferDto = txDtoAdapter.readJsonFrom("tx_details_transfer.json")
        coEvery { gatewayApi.loadTransactionDetails(any()) } returns txTransferDto
        val txTransfer = transactionRepository.getTransactionDetails("id")
        val executionInfo = txTransfer.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails

        val txCustomSafeTxHash = executionInfo.safeTxHash
        val calculatedTxHash = calculateSafeTxHash(safeAddress, txTransfer.toTransactionDetailsViewData(emptyList()), executionInfo)?.toHexString()?.addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    @Test
    fun `calculateSafeTxHash (safe, settingsChangeTx) should return same value as in settingsChangeTx`() = runBlocking {
        val txSettingsChangeDto = txDtoAdapter.readJsonFrom("tx_details_settings_change.json")
        coEvery { gatewayApi.loadTransactionDetails(any()) } returns txSettingsChangeDto
        val txSettingsChange = transactionRepository.getTransactionDetails("id")
        val executionInfo = txSettingsChange.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails

        val txCustomSafeTxHash = executionInfo.safeTxHash
        val calculatedTxHash = calculateSafeTxHash(safeAddress, txSettingsChange.toTransactionDetailsViewData(emptyList()), executionInfo)?.toHexString()?.addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

}
