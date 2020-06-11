package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class TransactionRepositoryTest {

    private val transactionServiceApi = mockk<TransactionServiceApi>()

    private val transactionRepository = TransactionRepository(transactionServiceApi)

    @Test
    fun `loadTransactions (api failure) should throw`() = runBlockingTest {
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
    fun `loadTransaction (valid address, single history eth transfer) should returned paged mapped list`() = runBlockingTest {
        val transactionDto = buildTransactionDto().copy(transfers = listOf(buildTransferDto()))
        val safeAddress = Solidity.Address(BigInteger.ONE)
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(safeAddress)

        assertEquals(actual.results.size, 1)
        assert(actual.results[0] is Transaction.Transfer)
        (actual.results[0] as Transaction.Transfer).let { transfer ->
            assertEquals(transactionDto.transfers?.get(0)?.to, transfer.recipient)
            assertEquals(transactionDto.transfers?.get(0)?.from, transfer.sender)
            assertEquals(transactionDto.transfers?.get(0)?.value, transfer.value)
            assertEquals(TokenRepository.ETH_SERVICE_TOKEN_INFO, transfer.tokenInfo)
        }
        coVerify(exactly = 1) { transactionServiceApi.loadTransactions(safeAddress.asEthereumAddressChecksumString()) }
    }

    private fun buildTransactionDto(): MultisigTransactionDto =
        MultisigTransactionDto(
            safe = Solidity.Address(BigInteger.ONE),
            to = Solidity.Address(BigInteger.ONE),
            value = BigInteger.ONE,
            nonce = BigInteger.ONE,
            safeTxGas = BigInteger.ONE,
            baseGas = BigInteger.ONE,
            gasPrice = BigInteger.ONE
        )

    private fun buildTransferDto(): TransferDto =
        TransferDto(
            to = Solidity.Address(BigInteger.ONE),
            from = Solidity.Address(BigInteger.ONE),
            type = TransferType.ETHER_TRANSFER,
            value = BigInteger.ONE
        )
}
