package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.*
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

//    @Test
//    fun `getTransactions (module transaction) should return Custom Transaction`() = runBlockingTest {
//        val safeAddress = Solidity.Address(BigInteger.ONE)
//        val transactionDto = buildModuleTransactionDto()
//        val pagedResult = listOf(transactionDto)
//        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)
//
//        val actual = transactionRepository.getTransactions(safeAddress)
//
//        with(actual.results[0] as Transaction.Custom) {
//
//            assert(address === transactionDto.module)
////                assertEquals(address, transactionDto.module)
//
//            //address
//            //data.size
//
//            //date
//            //value
//
//
//        }
//    }

    private fun buildModuleTransactionDto(
        to: Solidity.Address,
        module: Solidity.Address,
        safe: Solidity.Address
    ): ModuleTransactionDto =
        ModuleTransactionDto(to = to, module = module, safe = safe)

    private fun buildTransactionDto(): MultisigTransactionDto =
        MultisigTransactionDto(
            safe = Solidity.Address(BigInteger.ONE),
            to = Solidity.Address(BigInteger.ONE),
            operation = Operation.CALL,
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
