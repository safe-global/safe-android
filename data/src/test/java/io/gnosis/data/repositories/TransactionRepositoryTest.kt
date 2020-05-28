package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test
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
    fun `loadTransaction (valid address) should returned paged mapped list`() {
    }
}
