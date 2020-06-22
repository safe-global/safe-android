package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.*
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class TransactionRepositoryTest {

    private val transactionServiceApi = mockk<TransactionServiceApi>()

    private val transactionRepository = TransactionRepository(transactionServiceApi)
    private val defaultSafeAddress = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!
    private val defaultFromAddress = "0x7cd310A8AeBf268bF78ea16C601F201ca81e84Cc".asEthereumAddress()!!
    private val defaultToAddress = "0x2134Bb3DE97813678daC21575E7A77a95079FC51".asEthereumAddress()!!
    private val defaultValue = BigInteger("230000000000000000")

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
        val transactionDto = buildModuleTransactionDto(module = Solidity.Address(BigInteger.ONE))
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.module, address)
            assertEquals( 0L, dataSize)
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

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.to, address)
            assertEquals( 0L, dataSize)
        }
    }

    @Test
    fun `getTransactions (multisig transaction settings change) should return settings`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(
            to = defaultSafeAddress, safe = defaultSafeAddress,
            dataDecodedDto = DataDecodedDto("swapOwner", null)
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.SettingsChange) {
            assertEquals("swapOwner", dataDecoded.method)
            assertEquals(transactionDto.nonce, nonce)
            assertEquals(transactionDto.creationDate, date)
        }
    }

    @Test
    fun `getTransactions (multisig transaction for ERC20) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(
            contractInfoType = ContractInfoType.ERC20,
            dataDecodedDto = DataDecodedDto(
                "transfer",
                listOf(
                    ParamsDto("to", "address", defaultToAddress.asEthereumAddressChecksumString()),
                    ParamsDto("value", "uint256", defaultValue.asDecimalString())
                )
            )
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals("Expect one Transfer result", 1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals("Correct value expected: ", defaultValue, value)
            assertEquals("Correct date expected: ", transactionDto.creationDate, date)
            assertEquals("Correct safe expected: ", transactionDto.safe, sender)
            assertEquals("Correct to expected: ", transactionDto.to, recipient)
            //  TODO: check for right transfer type
            assertEquals(FAKE_ERC20_TOKEN_INFO, tokenInfo)
        }
    }

    @Test
    fun `getTransactions (multisig transaction for ERC20 with transferFrom) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(
            contractInfoType = ContractInfoType.ERC20,
            dataDecodedDto = DataDecodedDto(
                "transferFrom",
                listOf(
                    ParamsDto("from", "address", defaultFromAddress.asEthereumAddressChecksumString()),
                    ParamsDto("to", "address", defaultToAddress.asEthereumAddressChecksumString()),
                    ParamsDto("value", "uint256", defaultValue.asDecimalString())
                )
            )
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals("Expect one Transfer result", 1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals("Correct value expected: ", defaultValue, value)
            assertEquals("Correct date expected: ", transactionDto.creationDate, date)
            assertEquals("Correct sender expected: ", transactionDto.dataDecoded?.parameters?.getValueByName("from")?.asEthereumAddress(), sender)
            assertEquals("Correct to expected: ", transactionDto.to, recipient)
            //  TODO: check for right transfer type
            assertEquals(FAKE_ERC20_TOKEN_INFO, tokenInfo)
        }
    }

    @Test
    fun `getTransactions (multisig transaction for ERC721) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(
            contractInfoType = ContractInfoType.ERC721,
            dataDecodedDto = DataDecodedDto("safeTransferFrom", null) // TODO: check also transferFrom (might have differrent adress copied
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.creationDate, date)
            assertEquals(transactionDto.safe, sender)
            assertEquals(transactionDto.to, recipient)
            //  TODO: check for right transfer type
            assertEquals(FAKE_ERC721_TOKEN_INFO, tokenInfo)
        }

    }

    @Test
    fun `getTransactions (multisig transaction ETH transfer) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(transfers = listOf(buildTransferDto()))
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
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

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.to, address)
            assertEquals(0L, dataSize)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction with transfers ERC20, ERC721 and ETH) should return transfer list`() = runBlockingTest {
        val transactionDto = buildEthereumTransactionDto(
            transfers = listOf(
                buildTransferDto(TransferType.ERC20_TRANSFER),
                buildTransferDto(TransferType.ERC721_TRANSFER),
                buildTransferDto()
            )
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(3, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.blockTimestamp, date)
            assertEquals(transactionDto.from, sender)
            assertEquals(transactionDto.to, recipient)
            assertEquals(FAKE_ERC20_TOKEN_INFO, tokenInfo)
        }

        with(actual.results[1] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.blockTimestamp, date)
            assertEquals(transactionDto.from, sender)
            assertEquals(transactionDto.to, recipient)
            assertEquals(FAKE_ERC721_TOKEN_INFO, tokenInfo)
        }
        with(actual.results[2] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.blockTimestamp, date)
            assertEquals(transactionDto.from, sender)
            assertEquals(transactionDto.to, recipient)
            assertEquals(ETH_SERVICE_TOKEN_INFO, tokenInfo)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction no transfers and with data) should return custom`() = runBlockingTest {
        val transactionDto = buildEthereumTransactionDto(data = "0x6a7612020000000000000000000000001c8b9b78e3085866521fe2")
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.from, address)
            assertEquals(transactionDto.data?.dataSizeBytes(), dataSize)
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.blockTimestamp, date)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction with no transfers and no data with value) should return ETH transfer of value`() = runBlockingTest {
        val transactionDto = buildEthereumTransactionDto(data = "0x")
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.to, recipient)
            assertEquals(transactionDto.from, sender)
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.blockTimestamp, date)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction with no transfers and no data with no value) should return ETH transfer of 0 value`() =
        runBlockingTest {
            val transactionDto = buildEthereumTransactionDto(value = BigInteger.ZERO)
            val pagedResult = listOf(transactionDto)
            coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

            val actual = transactionRepository.getTransactions(defaultSafeAddress)

            coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
            assertEquals(1, actual.results.size)
            with(actual.results[0] as Transaction.Transfer) {
                assertEquals(transactionDto.to, recipient)
                assertEquals(transactionDto.from, sender)
                assertEquals(transactionDto.value, value)
                assertEquals(transactionDto.blockTimestamp, date)
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

    @Test
    fun `hexStringNullOrEmpty (is 0x) should return true`() {
        assertTrue("0x".hexStringNullOrEmpty())
    }

    @Test
    fun `hexStringNullOrEmpty (is null) should return true`() {
        assertTrue(null.hexStringNullOrEmpty())
    }

    @Test
    fun `hexStringNullOrEmpty (is 0x1A) should return false`() {
        assertFalse("0x1A".hexStringNullOrEmpty())
    }

    private fun buildModuleTransactionDto(
        to: Solidity.Address = defaultToAddress,
        module: Solidity.Address,
        safe: Solidity.Address = defaultSafeAddress
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
        safe: Solidity.Address = defaultSafeAddress,
        to: Solidity.Address = defaultToAddress,
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

    private fun buildEthereumTransactionDto(
        transfers: List<TransferDto> = listOf(),
        from: Solidity.Address = defaultFromAddress,
        to: Solidity.Address = defaultToAddress,
        data: String = "0x",
        value: BigInteger = BigInteger.ONE
    ): EthereumTransactionDto {
        return EthereumTransactionDto(
            from = from,
            to = to,
            value = value,
            transfers = transfers,
            data = data,
            txHash = "0x1234",
            blockTimestamp = null
        )
    }

    private fun buildTransferDto(
        type: TransferType = TransferType.ETHER_TRANSFER
    ): TransferDto =
        TransferDto(
            to = defaultToAddress,
            from = defaultFromAddress,
            type = type,
            value = BigInteger.ONE
        )
}
