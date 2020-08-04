package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.ConfirmationDto
import io.gnosis.data.backend.dto.ContractInfoDto
import io.gnosis.data.backend.dto.ContractInfoType
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.EthereumTransactionDto
import io.gnosis.data.backend.dto.ModuleTransactionDto
import io.gnosis.data.backend.dto.MultisigTransactionDto
import io.gnosis.data.backend.dto.Operation
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.backend.dto.SignatureType
import io.gnosis.data.backend.dto.TransferDto
import io.gnosis.data.backend.dto.TransferType
import io.gnosis.data.backend.dto.UnknownTransactionDto
import io.gnosis.data.models.Page
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.TransactionStatus
import io.gnosis.data.repositories.TokenRepository.Companion.ERC20_FALLBACK_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.TokenRepository.Companion.ERC721_FALLBACK_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.*

class TransactionRepositoryTest {

    private val transactionServiceApi = mockk<TransactionServiceApi>()

    private val transactionRepository = TransactionRepository(transactionServiceApi)
    private val defaultSafeAddress = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!
    private val defaultFromAddress = "0x7cd310A8AeBf268bF78ea16C601F201ca81e84Cc".asEthereumAddress()!!
    private val defaultToAddress = "0x2134Bb3DE97813678daC21575E7A77a95079FC51".asEthereumAddress()!!
    private val defaultValue = BigInteger("230000000000000000")
    private val defaultTokenId = "23"
    private val defaultSafeInfo = SafeInfo(
        defaultSafeAddress,
        BigInteger.TEN,
        10,
        emptyList(),
        Solidity.Address(BigInteger.ONE),
        emptyList(),
        Solidity.Address(BigInteger.ONE)
    )

    @Test
    fun `getTransactions (api failure) should throw`() = runBlockingTest {
        val throwable = Throwable()
        coEvery { transactionServiceApi.loadTransactions(any()) } throws throwable

        val actual = runCatching { transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo) }

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

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.module, address)
            assertEquals(0L, dataSize)
            assertEquals(transactionDto.created, date)
            assertEquals(transactionDto.value, value)
        }
    }

    @Test
    fun `getTransactions (UnknownTransaction with data) should return custom`() = runBlockingTest {
        val transactionDto = UnknownTransactionDto
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.to, address)
            assertEquals(0L, dataSize)
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

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

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

        val erc20ServiceTokenInfo = buildErc20ServiceTokenInfo()
        val erc20Transfer = buildTransferDto(tokenInfo = erc20ServiceTokenInfo)
        val transactionDto = buildMultisigTransactionDto(
            contractInfoType = null,
            dataDecodedDto = DataDecodedDto(
                "transfer",
                listOf(
                    ParamsDto("to", "address", defaultSafeAddress.asEthereumAddressChecksumString()),
                    ParamsDto("value", "uint256", defaultValue.asDecimalString())
                )
            ),
            transfers = listOf(erc20Transfer)
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals("Expect one Transfer result", 1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals("Correct value expected: ", defaultValue, value)
            assertEquals("Correct date expected: ", transactionDto.creationDate, date)
            assertEquals("Correct safe expected: ", defaultSafeAddress, sender)
            assertEquals("Correct to expected: ", defaultSafeAddress, recipient)
            //  TODO: check for right transfer type
            assertEquals(erc20ServiceTokenInfo, tokenInfo)
            assertEquals("Outgoing tx expected: ", false, incoming)
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
                    ParamsDto("to", "address", defaultSafeAddress.asEthereumAddressChecksumString()),
                    ParamsDto("value", "uint256", defaultValue.asDecimalString())
                )
            )
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals("Expect one Transfer result", 1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals("Correct value expected: ", defaultValue, value)
            assertEquals("Correct date expected: ", transactionDto.creationDate, date)
            assertEquals("Correct sender expected: ", transactionDto.dataDecoded?.parameters?.getValueByName("from")?.asEthereumAddress(), sender)
            assertEquals("Correct recipient expected: ", defaultSafeAddress, recipient)
            //  TODO: check for right transfer type
            assertEquals(ERC20_FALLBACK_SERVICE_TOKEN_INFO, tokenInfo)
            assertEquals("Incoming tx expected: ", true, incoming)
        }
    }

    @Test
    fun `getTransactions (multisig transaction for ERC721) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(
            dataDecodedDto = DataDecodedDto(
                "safeTransferFrom",
                listOf(
                    ParamsDto("from", "address", defaultFromAddress.asEthereumAddressChecksumString()),
                    ParamsDto("to", "address", defaultToAddress.asEthereumAddressChecksumString()),
                    ParamsDto("tokenId", "uint256", defaultTokenId)
                )
            ),
            transfers = listOf(buildTransferDto(tokenInfo = ERC721_FALLBACK_SERVICE_TOKEN_INFO))
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(BigInteger.ONE, value)
            assertEquals(transactionDto.creationDate, date)
            assertEquals(transactionDto.safe.asEthereumAddressChecksumString(), defaultSafeAddress.asEthereumAddressChecksumString())
            assertEquals(defaultFromAddress.asEthereumAddressChecksumString(), sender.asEthereumAddressChecksumString())
            assertEquals(defaultToAddress.asEthereumAddressChecksumString(), recipient.asEthereumAddressChecksumString())
            assertEquals(ERC721_FALLBACK_SERVICE_TOKEN_INFO, tokenInfo)
            assertEquals("Outgoing tx expected: ", false, incoming)
        }
    }

    @Test
    fun `getTransactions (multisig transaction ETH transfer) should return transfer`() = runBlockingTest {
        val transactionDto = buildMultisigTransactionDto(transfers = listOf(buildTransferDto()))
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.creationDate, date)
            assertEquals(transactionDto.safe, sender)
            assertEquals(transactionDto.to, recipient)
            assertEquals(ETH_SERVICE_TOKEN_INFO, tokenInfo)
            assertEquals("Outgoing tx expected: ", false, incoming)
        }
    }

    @Test
    fun `getTransactions (multisig unknown type) should return custom`() = runBlockingTest {

        val transactionDto = buildMultisigTransactionDto(operation = Operation.DELEGATE)
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.to, address)
            assertEquals(0L, dataSize)
        }
    }

    @Test
    fun `getTransactions (multisig !isSuccessful) should return failed custom transfer`() = runBlockingTest {

        val transactionDto = buildMultisigTransactionDto(
            operation = Operation.DELEGATE,
            isExecuted = true,
            isSuccessful = false
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.to, address)
            assertEquals(0L, dataSize)
            assertEquals(TransactionStatus.Failed, status)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction with transfers ERC20, ERC721 and ETH) should return transfer list`() = runBlockingTest {
        val transactionDto = buildEthereumTransactionDto(
            transfers = listOf(
                buildTransferDto(
                    TransferType.ERC20_TRANSFER,
                    executionDate = Date(),
                    tokenInfo = buildErc20ServiceTokenInfo()
                ),
                buildTransferDto(
                    TransferType.ERC721_TRANSFER,
                    executionDate = Date(),
                    value = BigInteger.ONE,
                    tokenInfo = ERC721_FALLBACK_SERVICE_TOKEN_INFO
                ),
                buildTransferDto(executionDate = Date())
            )
        )
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(3, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            val transferDto = transactionDto.transfers?.get(0)
            assertEquals(transferDto?.value, value)
            assertEquals(transferDto?.executionDate, date)
            assertEquals(transactionDto.from.asEthereumAddressChecksumString(), sender.asEthereumAddressChecksumString())
            assertEquals(transactionDto.to.asEthereumAddressChecksumString(), recipient.asEthereumAddressChecksumString())
            assertEquals(buildErc20ServiceTokenInfo(), tokenInfo)
            assertEquals("Outgoing tx expected: ", false, incoming)
        }
        with(actual.results[1] as Transaction.Transfer) {
            assertEquals(BigInteger.ONE, value)
            assertEquals(transactionDto.transfers?.get(1)?.executionDate, date)
            assertEquals(transactionDto.from.asEthereumAddressChecksumString(), sender.asEthereumAddressChecksumString())
            assertEquals(transactionDto.to.asEthereumAddressChecksumString(), recipient.asEthereumAddressChecksumString())
            assertEquals(ERC721_FALLBACK_SERVICE_TOKEN_INFO, tokenInfo)
            assertEquals("Outgoing tx expected: ", false, incoming)
        }
        with(actual.results[2] as Transaction.Transfer) {
            val transferDto = transactionDto.transfers?.get(2)
            assertEquals(transferDto?.value, value)
            assertEquals(transferDto?.executionDate, date)
            assertEquals(transactionDto.from.asEthereumAddressChecksumString(), sender.asEthereumAddressChecksumString())
            assertEquals(transactionDto.to.asEthereumAddressChecksumString(), recipient.asEthereumAddressChecksumString())
            assertEquals(ETH_SERVICE_TOKEN_INFO, tokenInfo)
            assertEquals("Outgoing tx expected: ", false, incoming)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction no transfers and with data) should return custom`() = runBlockingTest {
        val transactionDto = buildEthereumTransactionDto(data = "0x6a7612020000000000000000000000001c8b9b78e3085866521fe2")
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Custom) {
            assertEquals(transactionDto.to, address)
            assertEquals(transactionDto.data?.dataSizeBytes(), dataSize)
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.blockTimestamp, date)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction with no transfers and no data with value) should return ETH transfer of value`() = runBlockingTest {
        val transactionDto = buildEthereumTransactionDto(data = "0x", to = defaultSafeAddress)
        val pagedResult = listOf(transactionDto)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
        assertEquals(1, actual.results.size)
        with(actual.results[0] as Transaction.Transfer) {
            assertEquals(transactionDto.to, recipient)
            assertEquals(transactionDto.from, sender)
            assertEquals(transactionDto.value, value)
            assertEquals(transactionDto.blockTimestamp, date)
            assertEquals("Incoming tx expected: ", true, incoming)
        }
    }

    @Test
    fun `getTransactions (ethereum transaction with no transfers and no data with no value) should return ETH transfer of 0 value`() =
        runBlockingTest {
            val transactionDto = buildEthereumTransactionDto(value = BigInteger.ZERO, to = defaultSafeAddress)
            val pagedResult = listOf(transactionDto)
            coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

            val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

            coVerify { transactionServiceApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
            assertEquals(1, actual.results.size)
            with(actual.results[0] as Transaction.Transfer) {
                assertEquals(transactionDto.to, recipient)
                assertEquals(transactionDto.from, sender)
                assertEquals(transactionDto.value, value)
                assertEquals(transactionDto.blockTimestamp, date)
                assertEquals("Incoming tx expected: ", true, incoming)
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

    @Test
    fun `getTransactions - (Ethereum Transaction) should return status success`() = runBlocking {
        val transactionDto = buildEthereumTransactionDto()
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, listOf(transactionDto))

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        assertEquals(TransactionStatus.Success, actual.results[0].status)
    }

    @Test
    fun `getTransactions - (Module Transaction) should return status success`() = runBlocking {
        val transactionDto = buildModuleTransactionDto(module = Solidity.Address(BigInteger.TEN))
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, listOf(transactionDto))

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        assertEquals(TransactionStatus.Success, actual.results[0].status)
    }

    @Test
    fun `getTransactions - (Multisig Transaction, executed true, successful true) should return status success`() = runBlocking {
        val transactionDto = buildMultisigTransactionDto(isExecuted = true, isSuccessful = true)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, listOf(transactionDto))

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        assertEquals(TransactionStatus.Success, actual.results[0].status)
    }

    @Test
    fun `getTransactions - (Multisig Transaction, executed true, successful false) should return status failed`() = runBlocking {
        val transactionDto = buildMultisigTransactionDto(isExecuted = true, isSuccessful = false)
        coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, listOf(transactionDto))

        val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

        assertEquals(TransactionStatus.Failed, actual.results[0].status)
    }

    @Test
    fun `getTransactions - (Multisig Transaction, executed false, successful false, nonce lower than safe) should return status cancelled`() =
        runBlocking {
            val transactionDto = buildMultisigTransactionDto(isExecuted = false, isSuccessful = false, nonce = BigInteger.ONE)
            coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, listOf(transactionDto))

            val actual = transactionRepository.getTransactions(defaultSafeAddress, defaultSafeInfo)

            assertEquals(TransactionStatus.Cancelled, actual.results[0].status)
        }

    @Test
    fun `getTransactions - (Multisig Transaction, executed false, nonce greater than safe, with enough confirmations) should return status awaitingExecution`() =
        runBlocking {
            val confirmations = listOf(buildConfirmationDto(), buildConfirmationDto())
            val transactionDto = buildMultisigTransactionDto()
                .copy(isExecuted = false, nonce = BigInteger.TEN, confirmations = confirmations)
            val safeInfo = defaultSafeInfo.copy(nonce = BigInteger.ONE, threshold = 2)
            coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, listOf(transactionDto))

            val actual = transactionRepository.getTransactions(defaultSafeAddress, safeInfo)

            assertEquals(TransactionStatus.AwaitingExecution, actual.results[0].status)
        }

    @Test
    fun `getTransactions - (Multisig Transaction, executed false, nonce equal to safe, with enough confirmations) should return status awaitingExecution`() =
        runBlocking {
            val confirmations = listOf(buildConfirmationDto(), buildConfirmationDto())
            val transactionDto = buildMultisigTransactionDto()
                .copy(isExecuted = false, nonce = BigInteger.TEN, confirmations = confirmations)
            val safeInfo = defaultSafeInfo.copy(nonce = BigInteger.TEN, threshold = 2)
            coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, listOf(transactionDto))

            val actual = transactionRepository.getTransactions(defaultSafeAddress, safeInfo)

            assertEquals(TransactionStatus.AwaitingExecution, actual.results[0].status)
        }

    @Test
    fun `getTransactions - (Multisig Transaction, executed false, nonce greater than safe, with insufficient confirmations) should return status awaitingConfirmation`() =
        runBlocking {
            val confirmations = listOf(buildConfirmationDto(), buildConfirmationDto())
            val transactionDto = buildMultisigTransactionDto()
                .copy(isExecuted = false, nonce = BigInteger.TEN, confirmations = confirmations)
            val safeInfo = defaultSafeInfo.copy(nonce = BigInteger.TEN, threshold = 3)
            coEvery { transactionServiceApi.loadTransactions(any()) } returns Page(1, null, null, listOf(transactionDto))

            val actual = transactionRepository.getTransactions(defaultSafeAddress, safeInfo)

            assertEquals(TransactionStatus.AwaitingConfirmations, actual.results[0].status)
        }

    private fun buildMultisigTransactionDto(
        transfers: List<TransferDto>? = null,
        contractInfoType: ContractInfoType? = null,
        dataDecodedDto: DataDecodedDto? = null,
        safe: Solidity.Address = defaultSafeAddress,
        to: Solidity.Address = defaultToAddress,
        operation: Operation = Operation.CALL,
        isExecuted: Boolean = false,
        isSuccessful: Boolean = false,
        nonce: BigInteger = BigInteger.ONE
    ): MultisigTransactionDto {
        return MultisigTransactionDto(
            safe = safe,
            to = to,
            operation = operation,
            value = BigInteger.ONE,
            nonce = nonce,
            safeTxGas = BigInteger.ONE,
            baseGas = BigInteger.ONE,
            gasPrice = BigInteger.ONE,
            transfers = transfers,
            contractInfo = contractInfoType?.let { ContractInfoDto(it) },
            dataDecoded = dataDecodedDto,
            isExecuted = isExecuted,
            isSuccessful = isSuccessful
        )
    }

    private fun buildEthereumTransactionDto(
        transfers: List<TransferDto> = listOf(),
        from: Solidity.Address = defaultFromAddress,
        to: Solidity.Address = defaultToAddress,
        data: String = "0x",
        value: BigInteger = BigInteger.ZERO
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
        type: TransferType = TransferType.ETHER_TRANSFER,
        value: BigInteger = BigInteger.TEN,
        executionDate: Date = Date(),
        tokenInfo: ServiceTokenInfo? = null
    ): TransferDto =
        TransferDto(
            to = defaultToAddress,
            from = defaultFromAddress,
            type = type,
            value = value,
            executionDate = executionDate,
            tokenInfo = tokenInfo
        )

    private fun buildConfirmationDto(): ConfirmationDto =
        ConfirmationDto(
            owner = defaultSafeAddress,
            submissionDate = null,
            transactionHash = null,
            signature = "signature",
            signatureType = SignatureType.APPROVED_HASH
        )

    private fun buildErc20ServiceTokenInfo(): ServiceTokenInfo =
        ServiceTokenInfo(
            decimals = 18,
            symbol = "WETH",
            name = "Wrapped Ether",
            address = "0x1234".asEthereumAddress()!!,
            type = null,
            logoUri = ""
        )
}
