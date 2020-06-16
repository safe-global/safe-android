package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.*
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class TransactionRepositoryTest {

    private val transactionServiceApi = mockk<TransactionServiceApi>()

    private val transactionRepository = TransactionRepository(transactionServiceApi)
    private val defaultSafeAddress = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!

    @Test
    fun `getTransactions (api failure) should throw`() = runBlockingTest {
        val throwable = Throwable()
        coEvery { transactionServiceApi.loadTransactions(any()) } throws throwable

        val actual = runCatching { transactionRepository.getTransactions(defaultSafeAddress) }

        with(actual) {
            assert(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
    }

    @Test
    fun `getTransactions (module transaction) should return custom`() = runBlockingTest {
        val transactionDto = buildModuleTransactionDto(
            Solidity.Address(BigInteger.ONE),
            Solidity.Address(BigInteger.ONE),
            Solidity.Address(BigInteger.ONE)
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.module, address)
            assertEquals(transactionDto.data?.dataSizeBytes() ?: 0L, dataSize)
            assertEquals(transactionDto.created, date)
            assertEquals(transactionDto.value, value)
        }
    }

    @Test
    fun `getTransactions (UnknownTransaction with data) should return custom`() = runBlockingTest {
        val transactionDto = UnknownTransactionDto
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.to, address)
            assertEquals(transactionDto.data?.dataSizeBytes() ?: 0L, dataSize)
        }
    }

    @Test
    fun `getTransactions (multisig transaction settings change) should return settings`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(
            to = defaultSafeAddress, safe = defaultSafeAddress,
            dataDecodedDto = DataDecodedDto("swapOwner", null) // TODO pick a method at random?
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.SettingsChange) {
            assertEquals("swapOwner", dataDecoded.method)
            // TODO: nonce, executionDate
        }
    }

    @Test
    fun `getTransactions (multisig transaction for ERC20) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(
            transfers = listOf(buildTransferDto()),
            contractInfoType = ContractInfoType.ERC20,
            dataDecodedDto = DataDecodedDto("safeTransferFrom", null) // TODO: check also transferFrom (might have different adress copied
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.creationDate, date)
            assertEquals(transactionDto.safe, sender)
            assertEquals(transactionDto.to, recipient)
            //  TODO: check for right transfer type
//            assertEquals(ERC721_TOKEN_INFO, tokenInfo)
        }
    }

    @Test
    fun `getTransactions (multisig transaction for ERC721) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(
            transfers = listOf(buildTransferDto()),
            contractInfoType = ContractInfoType.ERC721,
            dataDecodedDto = DataDecodedDto("transfer", null) // TODO: check also transferFrom (might have differrent adress copied
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.creationDate, date)
            assertEquals(transactionDto.safe, sender)
            assertEquals(transactionDto.to, recipient)
            //  TODO: check for right transfer type
//            assertEquals(ERC20_TOKEN_INFO, tokenInfo)
        }

    }

    @Test
    fun `getTransactions (multisig transaction ETH transfer) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(transfers = listOf(buildTransferDto()))
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.creationDate, date)
            assertEquals(transactionDto.safe, sender)
            assertEquals(transactionDto.to, recipient)
            assertEquals(ETH_SERVICE_TOKEN_INFO, tokenInfo)
        }
    }

    @Test
    fun `getTransactions (multisig unknown type) should return custom`() = runBlockingTest {

        val transactionDto = buildMultisigTransactionDto(operation = Operation.DELEGATE)
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.to, address)
            assertEquals(transactionDto.data?.dataSizeBytes() ?: 0L, dataSize)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction with transfers ERC20, ERC721 and ETH) should return transfer list`() = runBlockingTest { }

    @Test
    fun `getTransactions (ethereum transaction no transfers and with data) should return custom`() = runBlockingTest { }

    @Test
    fun `getTransactions (ethereum transaction with no transfers and no data with value) should return ETH transfer of value`() = runBlockingTest { }

    @Test
    fun `getTransactions (ethereum transaction with no transfers and no data with no value) should return ETH transfer of 0 value`() =
        runBlockingTest { }

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

    private fun buildMultisigTransactionDto(
        transfers: List<TransferDto>? = null,
        contractInfoType: ContractInfoType? = null,
        dataDecodedDto: DataDecodedDto? = null,
        safe: Solidity.Address = Solidity.Address(BigInteger.ONE),
        to: Solidity.Address = Solidity.Address(BigInteger.TEN),
        operation: Operation = Operation.CALL
    ): MultisigTransactionDto {
        return MultisigTransactionDto(
            safe = safe,
            to = to,
            operation = operation,
            value = BigInteger.ONE,
            nonce = BigInteger.ONE,
            safeTxGas = BigInteger.ONE,
            baseGas = BigInteger.ONE,
            gasPrice = BigInteger.ONE,
            transfers = transfers,
            contractInfo = contractInfoType?.let { ContractInfoDto(it) },
            dataDecoded = dataDecodedDto
        )
    }

    private fun buildTransferDto(): TransferDto =
        TransferDto(
            to = Solidity.Address(BigInteger.TEN),
            from = Solidity.Address(BigInteger.ONE),
            type = TransferType.ETHER_TRANSFER,
            value = BigInteger.ONE
        )
}
