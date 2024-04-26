package io.gnosis.data.utils

import io.gnosis.data.BuildConfig
import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Chain
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.readJsonFrom
import io.gnosis.data.repositories.TransactionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import java.math.BigInteger

class SafeTxHashTest {

    private val gatewayApi = mockk<GatewayApi>()
    private val transactionRepository = TransactionRepository(gatewayApi)

    private val txDtoAdapter = dataMoshi.adapter(TransactionDetails::class.java)

    private val chain = Chain.DEFAULT_CHAIN
    private val safeAddress = "0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress()!!

    @Test
    fun `calculateSafeTxHash (safe, customTx) should return same value as in customTx`() = runBlocking {
        val txCustomDto = txDtoAdapter.readJsonFrom("tx_details_custom.json")
        coEvery { gatewayApi.loadTransactionDetails(transactionId = any(), chainId = any()) } returns txCustomDto
        val txCustom = transactionRepository.getTransactionDetails(CHAIN_ID, "id")
        val executionInfo = txCustom.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails

        val txCustomSafeTxHash = executionInfo.safeTxHash
        val calculatedTxHash = calculateSafeTxHash(
            SemVer(1, 1, 0),
            chain.chainId,
            safeAddress,
            txCustom,
            executionInfo
        ).toHexString().addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    @Test
    fun `calculateSafeTxHash (safe, transferTx) should return same value as in transferTx`() = runBlocking {
        val txTransferDto = txDtoAdapter.readJsonFrom("tx_details_transfer.json")
        coEvery { gatewayApi.loadTransactionDetails(transactionId = any(), chainId = any()) } returns txTransferDto
        val txTransfer = transactionRepository.getTransactionDetails(CHAIN_ID, "id")
        val executionInfo = txTransfer.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails

        val txCustomSafeTxHash = executionInfo.safeTxHash
        val calculatedTxHash = calculateSafeTxHash(
            SemVer(1, 1, 0),
            chain.chainId,
            safeAddress,
            txTransfer,
            executionInfo
        ).toHexString().addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    @Test
    fun `calculateSafeTxHash (safe, settingsChangeTx) should return same value as in settingsChangeTx`() = runBlocking {
        val txSettingsChangeDto = txDtoAdapter.readJsonFrom("tx_details_settings_change.json")
        coEvery { gatewayApi.loadTransactionDetails(transactionId = any(), chainId = any()) } returns txSettingsChangeDto
        val txSettingsChange = transactionRepository.getTransactionDetails(CHAIN_ID, "id")
        val executionInfo = txSettingsChange.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails

        val txCustomSafeTxHash = executionInfo.safeTxHash
        val calculatedTxHash = calculateSafeTxHash(
            SemVer(1, 1, 0),
            chain.chainId,
            safeAddress,
            txSettingsChange,
            executionInfo
        ).toHexString().addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    @Test
    fun `calculateSafeTxHash (v1_3_0 safe, settingsChangeTx) should return same value as in settingsChangeTx`() = runBlocking {
        val safeAddress = "0x6cf158C37354d8da5396fb6d3e965318CCf119c1".asEthereumAddress()!!
        val chainId = BigInteger("5")
        val txSettingsChangeDto = txDtoAdapter.readJsonFrom("tx_details_custom_set_guard.json")
        coEvery { gatewayApi.loadTransactionDetails(transactionId = any(), chainId = any()) } returns txSettingsChangeDto
        val txSettingsChange = transactionRepository.getTransactionDetails(chainId, "id")
        val executionInfo = txSettingsChange.detailedExecutionInfo as DetailedExecutionInfo.MultisigExecutionDetails

        val txCustomSafeTxHash = executionInfo.safeTxHash
        val calculatedTxHash = calculateSafeTxHash(
            SemVer(1, 3, 0),
            chainId,
            safeAddress,
            txSettingsChange,
            executionInfo
        ).toHexString().addHexPrefix()

        assertEquals(txCustomSafeTxHash, calculatedTxHash)
    }

    companion object {
        private val CHAIN_ID = BuildConfig.CHAIN_ID.toBigInteger()
    }
}
