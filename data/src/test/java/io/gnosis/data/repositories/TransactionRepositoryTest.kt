package io.gnosis.data.repositories

import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.dto.*
import io.gnosis.data.models.*
import io.gnosis.data.readJsonFrom
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger

@RunWith(Parameterized::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
class TransactionRepositoryTransferTest(
    private val direction: TransactionDirection
) {
    private val gatewayApi = mockk<GatewayApi>()
    private val transactionRepository = TransactionRepository(gatewayApi)
    private val defaultSafeAddress = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!

    companion object {
        @JvmStatic
        @Parameterized.Parameters()
        fun directions(): Array<TransactionDirection> {
            return TransactionDirection.values()
        }
    }

    @Test
    fun `getTransactions (all transfer types) should return Transfers`() = runBlockingTest {
        val pagedResult = listOf(
            buildGateTransactionDto(txInfo = buildTransferTxInfo(direction = direction, transferInfo = buildTransferInfoERC20())),
            buildGateTransactionDto(txInfo = buildTransferTxInfo(direction = direction, transferInfo = buildTransferInfoEther())),
            buildGateTransactionDto(txInfo = buildTransferTxInfo(direction = direction, transferInfo = buildTransferInfoERC721()))
        )
        coEvery { gatewayApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        assertEquals(3, actual.results.size)
        (0..2).forEach { i ->
            with(actual.results[i] as Transaction.Transfer) {
                assertEquals(pagedResult[i].executionInfo?.nonce, this.nonce)
                when ((pagedResult[i].txInfo as TransactionInfoDto.Transfer).transferInfo) {
                    is TransferInfoDto.Erc20Transfer -> {
                        assertEquals(TokenType.ERC20, this.tokenInfo?.tokenType)
                        val erc20Transfer = (pagedResult[i].txInfo as TransactionInfoDto.Transfer).transferInfo as TransferInfoDto.Erc20Transfer
                        assertEquals(erc20Transfer.value, value)
                        assertEquals(erc20Transfer.tokenAddress, tokenInfo?.address)
                        assertEquals(erc20Transfer.tokenSymbol, tokenInfo?.symbol)
                        assertEquals(erc20Transfer.tokenName, tokenInfo?.name)
                        assertEquals(erc20Transfer.decimals, tokenInfo?.decimals)
                        assertEquals(erc20Transfer.logoUri, tokenInfo?.logoUri)
                    }
                    is TransferInfoDto.Erc721Transfer -> {
                        assertEquals(TokenType.ERC721, this.tokenInfo?.tokenType)
                        assertEquals(1.toBigInteger(), value)
                        val erc721Transfer = (pagedResult[i].txInfo as TransactionInfoDto.Transfer).transferInfo as TransferInfoDto.Erc721Transfer
                        assertEquals(erc721Transfer.tokenAddress, tokenInfo?.address)
                        assertEquals(erc721Transfer.tokenSymbol, tokenInfo?.symbol)
                        assertEquals(erc721Transfer.tokenName, tokenInfo?.name)
                    }
                    is TransferInfoDto.EtherTransfer -> {
                        assertEquals(TokenType.ETHER, this.tokenInfo?.tokenType)
                        val etherTransfer = (pagedResult[i].txInfo as TransactionInfoDto.Transfer).transferInfo as TransferInfoDto.EtherTransfer
                        assertEquals(etherTransfer.value, value)
                    }
                }
            }
        }
    }
}

@kotlinx.coroutines.ExperimentalCoroutinesApi
class TransactionRepositoryTest {
    private val gatewayApi = mockk<GatewayApi>()

    private val moshiAdapter = dataMoshi.adapter(GateTransactionDetailsDto::class.java)

    private val transactionRepository = TransactionRepository(gatewayApi)
    private val defaultSafeAddress = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!

    @Test
    fun `getTransactions (api failure) should throw`() = runBlockingTest {
        val throwable = Throwable()
        coEvery { gatewayApi.loadTransactions(any()) } throws throwable

        val actual = runCatching { transactionRepository.getTransactions(defaultSafeAddress) }

        with(actual) {
            assert(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { gatewayApi.loadTransactions(defaultSafeAddress.asEthereumAddressChecksumString()) }
    }

    @Test
    fun `getTransactions (all tx types) should return respective tx type`() = runBlockingTest {
        val pagedResult = listOf(
            buildGateTransactionDto(txInfo = buildTransferTxInfo()),
            buildGateTransactionDto(txInfo = buildCustomTxInfo()),
            buildGateTransactionDto(txInfo = buildSettingsChangeTxInfo()),
            buildGateTransactionDto(txInfo = buildCreationTxInfo())

        )
        coEvery { gatewayApi.loadTransactions(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getTransactions(defaultSafeAddress)

        assertEquals(4, actual.results.size)
        (0..3).forEach { i ->
            with(actual.results[i]) {
                when (pagedResult[i].txInfo) {
                    is TransactionInfoDto.Transfer -> {
                        assertEquals(pagedResult[i].executionInfo?.nonce, (this as Transaction.Transfer).nonce)
                        assertEquals(pagedResult[i].txStatus, this.status)
                        assertEquals((pagedResult[i].txInfo as TransactionInfoDto.Transfer).direction != TransactionDirection.OUTGOING, incoming)
                    }
                    is TransactionInfoDto.Custom -> {
                        assertEquals(pagedResult[i].executionInfo?.nonce, (this as Transaction.Custom).nonce)
                        assertEquals(pagedResult[i].txStatus, this.status)
                        assertEquals((pagedResult[i].txInfo as TransactionInfoDto.Custom).dataSize, dataSize)
                    }
                    is TransactionInfoDto.SettingsChange -> {
                        assertEquals(pagedResult[i].executionInfo?.nonce, (this as Transaction.SettingsChange).nonce)
                        assertEquals(pagedResult[i].txStatus, this.status)
                        assertEquals((pagedResult[i].txInfo as TransactionInfoDto.SettingsChange).dataDecoded, dataDecoded)
                    }
                    is TransactionInfoDto.Creation -> {
                        assertEquals(pagedResult[i].timestamp, (this as Transaction.Creation).timestamp.time)
                        assertEquals(pagedResult[i].txStatus, this.status)
                        assertEquals((pagedResult[i].txInfo as TransactionInfoDto.Creation).creator, (txInfo as TransactionInfo.Creation).creator)
                        assertEquals((pagedResult[i].txInfo as TransactionInfoDto.Creation).factory, (txInfo as TransactionInfo.Creation).factory)
                        assertEquals(
                            (pagedResult[i].txInfo as TransactionInfoDto.Creation).implementation,
                            (txInfo as TransactionInfo.Creation).implementation
                        )
                        assertEquals(
                            (pagedResult[i].txInfo as TransactionInfoDto.Creation).transactionHash,
                            (txInfo as TransactionInfo.Creation).transactionHash
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `loadTransactionsPage (all tx type) should return respective tx type`() = runBlockingTest {
        val pagedResult = listOf(
            buildGateTransactionDto(txInfo = buildTransferTxInfo()),
            buildGateTransactionDto(txInfo = buildCustomTxInfo()),
            buildGateTransactionDto(txInfo = buildSettingsChangeTxInfo())
        )
        coEvery { gatewayApi.loadTransactionsPage(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.loadTransactionsPage("url")

        assertEquals(3, actual.results.size)

        (0..2).forEach { i ->
            with(actual.results[i]) {
                when (pagedResult[i].txInfo) {
                    is TransactionInfoDto.Transfer -> {
                        assertEquals(pagedResult[i].executionInfo?.nonce, (this as Transaction.Transfer).nonce)
                        assertEquals(pagedResult[i].txStatus, this.status)
                        assertEquals((pagedResult[i].txInfo as TransactionInfoDto.Transfer).direction == TransactionDirection.INCOMING, incoming)
                    }
                    is TransactionInfoDto.Custom -> {
                        assertEquals(pagedResult[i].executionInfo?.nonce, (this as Transaction.Custom).nonce)
                        assertEquals(pagedResult[i].txStatus, this.status)
                        assertEquals((pagedResult[i].txInfo as TransactionInfoDto.Custom).dataSize, dataSize)
                    }
                    is TransactionInfoDto.SettingsChange -> {
                        assertEquals(pagedResult[i].executionInfo?.nonce, (this as Transaction.SettingsChange).nonce)
                        assertEquals(pagedResult[i].txStatus, this.status)
                        assertEquals((pagedResult[i].txInfo as TransactionInfoDto.SettingsChange).dataDecoded, dataDecoded)
                    }
                }
            }
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

    @Test
    fun `getValueByName (single param) should return right value`() {
        val params = listOf(ParamDto.ValueParam(name = "foo", type = "uint256", value = "12"))
        val result = params.getIntValueByName("foo")
        assertEquals("12", result)
    }

    @Test
    fun `getValueByName (several params) should return right value`() {
        val params = listOf(
            ParamDto.ValueParam(name = "foo", type = "uint256", value = "1"),
            ParamDto.ValueParam(name = "bar", type = "uint256", value = "2"),
            ParamDto.ValueParam(name = "baz", type = "uint256", value = "3")
        )

        val result = params.getIntValueByName("bar")

        assertEquals("2", result)
    }

    @Test
    fun `getValueByName (unavailable name) should return null`() {
        val params = listOf(ParamDto.ValueParam(name = "foo", type = "uint256", value = "12"))
        val result = params.getIntValueByName("bar")
        assertEquals(null, result)
    }

    @Test
    fun `submitConfirmation (API failure) should throw`() = runBlockingTest {
        val throwable = Throwable()
        val confirmationRequest = TransactionConfirmationRequest("0x0")
        coEvery { gatewayApi.submitConfirmation(any(), any()) } throws throwable

        val actual = runCatching { gatewayApi.submitConfirmation("0x0", confirmationRequest) }

        assert(actual.isFailure)
        assert(actual.exceptionOrNull() == throwable)
        coVerify { gatewayApi.submitConfirmation("0x0", confirmationRequest) }
    }

    @Test
    fun `submitConfirmation (successful) should return TransactionDetails`() = runBlockingTest {
        val transactionDetailsDto = moshiAdapter.readJsonFrom("tx_details_transfer.json")
        coEvery { gatewayApi.submitConfirmation(any(), any()) } returns transactionDetailsDto
        coEvery { gatewayApi.loadTransactionDetails(any()) } returns transactionDetailsDto
        val expected = transactionRepository.getTransactionDetails("txId")

        val actual = runCatching { transactionRepository.submitConfirmation("0x0", "0x0") }

        assert(actual.isSuccess)
        assert(actual.getOrNull() == expected)
        coVerify { gatewayApi.submitConfirmation("0x0", TransactionConfirmationRequest("0x0")) }
    }

    @Test
    fun `sign(successful) should return string`() {
        val ownerKey = "0xda18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexAsBigInteger()
        val actual = transactionRepository.sign(ownerKey, "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67")
        val expected =
            "d757e1a0f195a26988290d6f533c3cd5eff0924abe7f92c071ecbe007031a4274dd9aee773cf051a7c0906a332571dfbeda672cf98d2631d12e11764508e4ec81c"

        assertEquals(expected, actual)
    }
}

private fun buildGateTransactionDto(
    id: String = "1234",
    status: TransactionStatus = TransactionStatus.SUCCESS,
    txInfo: TransactionInfoDto = buildTransferTxInfo()
): GateTransactionDto =
    GateTransactionDto(
        id = id,
        txStatus = status,
        txInfo = txInfo,
        executionInfo = ExecutionInfoDto(
            nonce = 1.toBigInteger(),
            confirmationsSubmitted = 2,
            confirmationsRequired = 2,
            missingSigners = null
        ),
        timestamp = 1L
    )

private fun buildCustomTxInfo(
    value: BigInteger = BigInteger.ONE,
    to: Solidity.Address = "0x1234".asEthereumAddress()!!,
    dataSize: Int = 96
): TransactionInfoDto.Custom =
    TransactionInfoDto.Custom(
        type = GateTransactionType.SettingsChange,
        value = value,
        to = to,
        dataSize = dataSize
    )

private fun buildCreationTxInfo(
    factory: Solidity.Address = "0x12".asEthereumAddress()!!,
    creator: Solidity.Address = "0x1234".asEthereumAddress()!!,
    implementation: Solidity.Address = "0x123456".asEthereumAddress()!!,
    hash: String = "0x12345678"
): TransactionInfoDto.Creation =
    TransactionInfoDto.Creation(
        type = GateTransactionType.Creation,
        transactionHash = hash,
        implementation = implementation,
        factory = factory,
        creator = creator
    )

private fun buildSettingsChangeTxInfo(
    dataDecoded: DataDecodedDto = DataDecodedDto("defaultMethod", listOf())
): TransactionInfoDto.SettingsChange =
    TransactionInfoDto.SettingsChange(
        type = GateTransactionType.SettingsChange,
        dataDecoded = dataDecoded
    )

private fun buildTransferTxInfo(
    sender: Solidity.Address = "0x7cd310A8AeBf268bF78ea16C601F201ca81e84Cc".asEthereumAddress()!!,
    recipient: Solidity.Address = "0x2134Bb3DE97813678daC21575E7A77a95079FC51".asEthereumAddress()!!,
    direction: TransactionDirection = TransactionDirection.OUTGOING,
    transferInfo: TransferInfoDto = buildTransferInfoERC20()
): TransactionInfoDto.Transfer =
    TransactionInfoDto.Transfer(
        type = GateTransactionType.Transfer,
        sender = sender,
        recipient = recipient,
        direction = direction,
        transferInfo = transferInfo
    )

private fun buildTransferInfoERC20(
    value: BigInteger = "10000000".toBigInteger(),
    symbol: String = "WETH",
    name: String = "Wrapped Ether",
    address: Solidity.Address = "0x1234".asEthereumAddress()!!,
    uri: String = "https://www.erc20",
    decimals: Int? = 18
): TransferInfoDto =
    TransferInfoDto.Erc20Transfer(
        type = GateTransferType.ERC20,
        value = value,
        tokenSymbol = symbol,
        tokenName = name,
        tokenAddress = address,
        logoUri = uri,
        decimals = decimals
    )

private fun buildTransferInfoERC721(
    symbol: String = "CK",
    name: String = "Crypto Kitties",
    address: Solidity.Address = "0x123456".asEthereumAddress()!!,
    uri: String = "https://www.erc721",
    id: String = "id"
): TransferInfoDto =
    TransferInfoDto.Erc721Transfer(
        type = GateTransferType.ERC721,
        tokenSymbol = symbol,
        tokenName = name,
        tokenAddress = address,
        logoUri = uri,
        tokenId = id
    )

private fun buildTransferInfoEther(
    value: BigInteger = "23".toBigInteger()
): TransferInfoDto = TransferInfoDto.EtherTransfer(
    type = GateTransferType.ETHER,
    value = value
)
