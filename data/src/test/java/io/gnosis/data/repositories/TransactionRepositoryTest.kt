package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.models.*
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class TransactionRepositoryTest {

    private val transactionServiceApi = mockk<TransactionServiceApi>()

    private val transactionRepository = TransactionRepository(transactionServiceApi)

    @Test
    fun `getTransactions (api failure) should throw`() = runBlockingTest {
        val throwable = Throwable()
        val safeAddress = Solidity.Address(BigInteger.ONE)
        coEvery { transactionServiceApi.loadTransactions(any()) } throws throwable

        val actual = runCatching { transactionRepository.getTransactions(safeAddress) }

        with(actual) {
            assert(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { transactionServiceApi.loadTransactions(safeAddress.asEthereumAddressString()) }
    }

    @Test
    fun `getTransactions (module transaction) should return Custom Transaction`() = runBlockingTest {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val transactionDto = buildModuleTransactionDto(
            Solidity.Address(BigInteger.ONE),
            Solidity.Address(BigInteger.ONE),
            Solidity.Address(BigInteger.ONE)
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(safeAddress)

        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.module, address)
            assertEquals(transactionDto.data?.dataSizeBytes() ?: 0L, dataSize)
            assertEquals(transactionDto.created, date)
            assertEquals(transactionDto.value, value)
        }
    }

    @Test
    fun `getTransactions (multisig transaction with one outgoing ETH transfer) should return one outgoing transfer`() = runBlockingTest {
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val transactionDto = buildMultisigTransactionDto(transfers = listOf(buildTransferDto()))
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(safeAddress)

        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.creationDate, date)
            assertEquals(transactionDto.safe, sender)
            assertEquals(transactionDto.to, recipient)
            assertEquals(ETH_SERVICE_TOKEN_INFO, tokenInfo )
        }
    }

    @Test
    fun `dataSizeBytes (one byte data) should return 1`() {
        assertEquals("0x0A".dataSizeBytes(), 1L)
    }

    @Test
    fun `dataSizeBytes (zero byte data) should return 0`() {
        assertEquals("0x".dataSizeBytes(), 0L)
    }

    @Test
    fun `dataSizeBytes (5 byte data) should return 5`() {
        assertEquals("0x0123456789".dataSizeBytes(), 5L)
    }

    private fun buildModuleTransactionDto(
        to: Solidity.Address,
        module: Solidity.Address,
        safe: Solidity.Address
    ): ModuleTransactionDto =
        ModuleTransactionDto(
            to = to,
            module = module,
            safe = safe,
            value = BigInteger.ZERO
        )

    private fun buildMultisigTransactionDto(transfers: List<TransferDto>): MultisigTransactionDto =
        MultisigTransactionDto(
            safe = Solidity.Address(BigInteger.ONE),
            to = Solidity.Address(BigInteger.ONE),
            operation = Operation.CALL,
            value = BigInteger.ONE,
            nonce = BigInteger.ONE,
            safeTxGas = BigInteger.ONE,
            baseGas = BigInteger.ONE,
            gasPrice = BigInteger.ONE,
            transfers = transfers
        )

    private fun buildTransferDto(): TransferDto =
        TransferDto(
            to = Solidity.Address(BigInteger.TEN),
            from = Solidity.Address(BigInteger.ONE),
            type = TransferType.ETHER_TRANSFER,
            value = BigInteger.ONE
        )
}
