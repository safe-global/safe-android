package io.gnosis.data.repositories

import io.gnosis.data.BuildConfig
import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Page
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.data.readJsonFrom
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import java.util.*

@RunWith(Parameterized::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
class TransactionRepositoryTransferTest(
    private val direction: TransactionDirection
) {
    private val gatewayApi = mockk<GatewayApi>()
    private val transactionRepository = TransactionRepository(gatewayApi)
    private val defaultSafeAddress = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!
    private val defaultSafe = Safe(defaultSafeAddress, "Name", CHAIN_ID)

    companion object {
        @JvmStatic
        @Parameterized.Parameters()
        fun directions(): Array<TransactionDirection> {
            return TransactionDirection.values()
        }
    }
    @Test
    fun `getHistoryTransactions (all transfer types) should return Transfers`() = runTest(UnconfinedTestDispatcher()) {
        val pagedResult = listOf(
            buildGateTransaction(txInfo = buildTransferTxInfo(direction = direction, transferInfo = buildTransferInfoERC20())),
            buildGateTransaction(txInfo = buildTransferTxInfo(direction = direction, transferInfo = buildTransferInfoEther())),
            buildGateTransaction(txInfo = buildTransferTxInfo(direction = direction, transferInfo = buildTransferInfoERC721()))
        )
        coEvery { gatewayApi.loadTransactionsQueue(address = any(), chainId = any()) } returns Page(1, null, null, emptyList())
        coEvery { gatewayApi.loadTransactionsHistory(address = any(), chainId = any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.getHistoryTransactions(defaultSafe)

        assertEquals(3, actual.results.size)
        (0..2).forEach { i ->
            val actualTransaction = actual.results[i] as TxListEntry.Transaction
            assertEquals(
                (pagedResult[i] as TxListEntry.Transaction).transaction.executionInfo?.nonce,
                actualTransaction.transaction.executionInfo?.nonce
            )
            val txInfo = ((pagedResult[i] as TxListEntry.Transaction).transaction.txInfo as TransactionInfo.Transfer)
            when (val transferInfo = txInfo.transferInfo) {
                is TransferInfo.Erc20Transfer -> {
                    val erc20Transfer = txInfo.transferInfo as TransferInfo.Erc20Transfer
                    assertEquals(erc20Transfer.value, transferInfo.value())
                    assertEquals(erc20Transfer.tokenAddress, transferInfo.tokenAddress)
                    assertEquals(erc20Transfer.tokenSymbol, transferInfo.tokenSymbol)
                    assertEquals(erc20Transfer.tokenName, transferInfo.tokenName)
                    assertEquals(erc20Transfer.decimals, transferInfo.decimals)
                    assertEquals(erc20Transfer.logoUri, transferInfo.logoUri)
                }
                is TransferInfo.Erc721Transfer -> {
                    assertEquals(1.toBigInteger(), transferInfo.value())
                    val erc721Transfer = txInfo.transferInfo as TransferInfo.Erc721Transfer
                    assertEquals(erc721Transfer.tokenAddress, transferInfo.tokenAddress)
                    assertEquals(erc721Transfer.tokenSymbol, transferInfo.tokenSymbol)
                    assertEquals(erc721Transfer.tokenName, transferInfo.tokenName)
                    assertEquals(erc721Transfer.tokenId, transferInfo.tokenId)
                }
                is TransferInfo.NativeTransfer -> {
                    val etherTransfer = txInfo.transferInfo as TransferInfo.NativeTransfer
                    assertEquals(etherTransfer.value, transferInfo.value())
                }
            }
        }
    }
}

@kotlinx.coroutines.ExperimentalCoroutinesApi
class TransactionRepositoryTest {
    private val gatewayApi = mockk<GatewayApi>()

    private val moshiAdapter = dataMoshi.adapter(TransactionDetails::class.java)

    private val transactionRepository = TransactionRepository(gatewayApi)
    private val defaultSafeAddress = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!
    private val defaultSafe = Safe(defaultSafeAddress, "Name", CHAIN_ID)

    @Test
    fun `getHistoryTransactions (api failure) should throw`() = runTest(UnconfinedTestDispatcher()) {
        val throwable = Throwable()
        coEvery { gatewayApi.loadTransactionsHistory(address = any(), chainId = any()) } throws throwable

        val actual = runCatching { transactionRepository.getHistoryTransactions(defaultSafe) }

        with(actual) {
            assertTrue(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) {
            gatewayApi.loadTransactionsHistory(
                address = defaultSafeAddress.asEthereumAddressChecksumString(),
                chainId = CHAIN_ID
            )
        }
    }

    @Test
    fun `getHistoryTransactions (all tx types) should return respective tx type`() = runTest(UnconfinedTestDispatcher()) {
        val pagedResult = listOf(
            buildGateTransaction(txInfo = buildTransferTxInfo()),
            buildGateTransaction(txInfo = buildCustomTxInfo()),
            buildGateTransaction(txInfo = buildSettingsChangeTxInfo()),
            buildGateTransaction(txInfo = buildCreationTxInfo())

        )
        coEvery { gatewayApi.loadTransactionsHistory(address = any(), chainId = any()) } returns Page(1, null, null, pagedResult)
        coEvery { gatewayApi.loadTransactionsQueue(address = any(), chainId = any()) } returns Page(1, null, null, emptyList())

        val actual = transactionRepository.getHistoryTransactions(defaultSafe)

        assertEquals(4, actual.results.size)
        (0..3).forEach { i ->
            val actualTransaction = actual.results[i] as TxListEntry.Transaction
            assertEquals(
                (pagedResult[i] as TxListEntry.Transaction).transaction.executionInfo?.nonce,
                actualTransaction.transaction.executionInfo?.nonce
            )
            val transaction = (pagedResult[i] as TxListEntry.Transaction).transaction
            when (val txInfo = transaction.txInfo) {
                is TransactionInfo.Transfer -> {
                    assertEquals(transaction.executionInfo?.nonce, actualTransaction.transaction.executionInfo?.nonce)
                    assertEquals(transaction.txStatus, actualTransaction.transaction.txStatus)
                }
                is TransactionInfo.Custom -> {
                    assertEquals(transaction.executionInfo?.nonce, actualTransaction.transaction.executionInfo?.nonce)
                    assertEquals(transaction.txStatus, actualTransaction.transaction.txStatus)
                    assertEquals((transaction.txInfo as TransactionInfo.Custom).dataSize, txInfo.dataSize)
                }
                is TransactionInfo.SettingsChange -> {
                    assertEquals(transaction.executionInfo?.nonce, actualTransaction.transaction.executionInfo?.nonce)
                    assertEquals(transaction.txStatus, actualTransaction.transaction.txStatus)
                    assertEquals((transaction.txInfo as TransactionInfo.SettingsChange).dataDecoded, txInfo.dataDecoded)
                }
                is TransactionInfo.Creation -> {
                    assertEquals(transaction.timestamp, actualTransaction.transaction.timestamp)
                    assertEquals(transaction.txStatus, actualTransaction.transaction.txStatus)
                    assertEquals((transaction.txInfo as TransactionInfo.Creation).creator, txInfo.creator)
                    assertEquals((transaction.txInfo as TransactionInfo.Creation).factory, txInfo.factory)
                    assertEquals((transaction.txInfo as TransactionInfo.Creation).implementation, txInfo.implementation)
                    assertEquals((transaction.txInfo as TransactionInfo.Creation).transactionHash, txInfo.transactionHash)
                }
                is TransactionInfo.Unknown -> {
                    // ignore
                }
            }
        }
    }


    @Test
    fun `loadTransactionsPage (all tx type) should return respective tx type`() = runTest(UnconfinedTestDispatcher()) {
        val pagedResult = listOf(
            buildGateTransaction(txInfo = buildTransferTxInfo()),
            buildGateTransaction(txInfo = buildCustomTxInfo()),
            buildGateTransaction(txInfo = buildSettingsChangeTxInfo())
        )
        coEvery { gatewayApi.loadTransactionsPage(any()) } returns Page(1, null, null, pagedResult)

        val actual = transactionRepository.loadTransactionsPage("url")

        assertEquals(3, actual.results.size)

        (0..2).forEach { i ->
            val actualTransaction = actual.results[i] as TxListEntry.Transaction
            assertEquals(
                (pagedResult[i] as TxListEntry.Transaction).transaction.executionInfo?.nonce,
                actualTransaction.transaction.executionInfo?.nonce
            )
            val pagedTransaction = (pagedResult[i] as TxListEntry.Transaction).transaction
            when (val txInfo = pagedTransaction.txInfo) {
                is TransactionInfo.Transfer -> {
                    assertEquals(pagedTransaction.executionInfo?.nonce, actualTransaction.transaction.executionInfo?.nonce)
                    assertEquals(pagedTransaction.txStatus, actualTransaction.transaction.txStatus)
                }
                is TransactionInfo.Custom -> {
                    assertEquals(pagedTransaction.executionInfo?.nonce, actualTransaction.transaction.executionInfo?.nonce)
                    assertEquals(pagedTransaction.txStatus, actualTransaction.transaction.txStatus)
                    assertEquals((pagedTransaction.txInfo as TransactionInfo.Custom).dataSize, txInfo.dataSize)
                }
                is TransactionInfo.SettingsChange -> {
                    assertEquals(pagedTransaction.executionInfo?.nonce, actualTransaction.transaction.executionInfo?.nonce)
                    assertEquals(pagedTransaction.txStatus, actualTransaction.transaction.txStatus)
                    assertEquals((pagedTransaction.txInfo as TransactionInfo.SettingsChange).dataDecoded, txInfo.dataDecoded)
                }
                else -> {
                    // Ignored
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
        val params = listOf(Param.Value(name = "foo", type = "uint256", value = "12"))
        val result = params.getIntValueByName("foo")
        assertEquals("12", result)
    }

    @Test
    fun `getValueByName (several params) should return right value`() {
        val params = listOf(
            Param.Value(name = "foo", type = "uint256", value = "1"),
            Param.Value(name = "bar", type = "uint256", value = "2"),
            Param.Value(name = "baz", type = "uint256", value = "3")
        )

        val result = params.getIntValueByName("bar")

        assertEquals("2", result)
    }

    @Test
    fun `getValueByName (unavailable name) should return null`() {
        val params = listOf(Param.Value(name = "foo", type = "uint256", value = "12"))
        val result = params.getIntValueByName("bar")
        assertEquals(null, result)
    }

    @Test
    fun `submitConfirmation (API failure) should throw`() = runTest(UnconfinedTestDispatcher()) {
        val throwable = Throwable()
        val confirmationRequest = TransactionConfirmationRequest("0x0")
        coEvery { gatewayApi.submitConfirmation(safeTxHash = any(), txConfirmationRequest = any(), chainId = any()) } throws throwable

        val actual =
            runCatching { gatewayApi.submitConfirmation(safeTxHash = "0x0", txConfirmationRequest = confirmationRequest, chainId = CHAIN_ID) }

        assertTrue(actual.isFailure)
        assertTrue(actual.exceptionOrNull() == throwable)
        coVerify { gatewayApi.submitConfirmation(safeTxHash = "0x0", txConfirmationRequest = confirmationRequest, chainId = CHAIN_ID) }
    }

    @Test
    fun `submitConfirmation (successful) should return TransactionDetails`() = runTest(UnconfinedTestDispatcher()) {
        val transactionDetailsDto = moshiAdapter.readJsonFrom("tx_details_transfer.json")
        coEvery { gatewayApi.submitConfirmation(safeTxHash = any(), txConfirmationRequest = any(), chainId = CHAIN_ID) } returns transactionDetailsDto
        coEvery { gatewayApi.loadTransactionDetails(transactionId = any(), chainId = any()) } returns transactionDetailsDto
        val expected = transactionRepository.getTransactionDetails(chainId = CHAIN_ID, txId = "txId")

        val actual = runCatching { transactionRepository.submitConfirmation("0x0", "0x0", CHAIN_ID) }

        assertTrue(actual.isSuccess)
        assertTrue(actual.getOrNull() == expected)
        coVerify { gatewayApi.submitConfirmation(safeTxHash = "0x0", txConfirmationRequest = TransactionConfirmationRequest("0x0"), chainId = CHAIN_ID) }
    }

    @Test
    fun `sign(successful) should return string`() {
        val ownerKey = "0xda18066dda40499e6ef67a392eda0fd90acf804448a765db9fa9b6e7dd15c322".hexAsBigInteger()
        val actual = transactionRepository.sign(ownerKey, "0xb3bb5fe5221dd17b3fe68388c115c73db01a1528cf351f9de4ec85f7f8182a67")
        val expected =
            "0xd757e1a0f195a26988290d6f533c3cd5eff0924abe7f92c071ecbe007031a4274dd9aee773cf051a7c0906a332571dfbeda672cf98d2631d12e11764508e4ec81c"

        assertEquals(expected, actual)
    }
}

private fun buildGateTransaction(
    id: String = "1234",
    status: TransactionStatus = TransactionStatus.SUCCESS,
    txInfo: TransactionInfo = buildTransferTxInfo()
): TxListEntry = TxListEntry.Transaction(
    transaction = Transaction(
        id = id,
        txStatus = status,
        txInfo = txInfo,
        executionInfo = ExecutionInfo(
            nonce = 1.toBigInteger(),
            confirmationsSubmitted = 2,
            confirmationsRequired = 2,
            missingSigners = null
        ),
        timestamp = Date(),
        safeAppInfo = null
    ), conflictType = ConflictType.None
)

private fun buildCustomTxInfo(
    value: BigInteger = BigInteger.ONE,
    to: Solidity.Address = "0x1234".asEthereumAddress()!!,
    dataSize: Int = 96
): TransactionInfo.Custom =
    TransactionInfo.Custom(
        value = value,
        to = AddressInfo(to),
        dataSize = dataSize,
        methodName = null,
        isCancellation = false
    )

private fun buildCreationTxInfo(
    factory: Solidity.Address = "0x12".asEthereumAddress()!!,
    creator: Solidity.Address = "0x1234".asEthereumAddress()!!,
    implementation: Solidity.Address = "0x123456".asEthereumAddress()!!,
    hash: String = "0x12345678"
): TransactionInfo.Creation =
    TransactionInfo.Creation(
        transactionHash = hash,
        implementation = AddressInfo(implementation),
        factory = AddressInfo(factory),
        creator = AddressInfo(creator)
    )

private fun buildSettingsChangeTxInfo(
    dataDecoded: DataDecoded = DataDecoded(
        "defaultMethod",
        listOf()
    )
): TransactionInfo.SettingsChange =
    TransactionInfo.SettingsChange(
        dataDecoded = dataDecoded,
        settingsInfo = null
    )

private fun buildTransferTxInfo(
    sender: Solidity.Address = "0x7cd310A8AeBf268bF78ea16C601F201ca81e84Cc".asEthereumAddress()!!,
    recipient: Solidity.Address = "0x2134Bb3DE97813678daC21575E7A77a95079FC51".asEthereumAddress()!!,
    direction: TransactionDirection = TransactionDirection.OUTGOING,
    transferInfo: TransferInfo = buildTransferInfoERC20()
): TransactionInfo.Transfer =
    TransactionInfo.Transfer(
        sender = AddressInfo(sender),
        recipient = AddressInfo(recipient),
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
): TransferInfo =
    TransferInfo.Erc20Transfer(
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
): TransferInfo =
    TransferInfo.Erc721Transfer(
        tokenSymbol = symbol,
        tokenName = name,
        tokenAddress = address,
        logoUri = uri,
        tokenId = id
    )

private fun buildTransferInfoEther(
    value: BigInteger = "23".toBigInteger()
): TransferInfo = TransferInfo.NativeTransfer(
    value = value
)

private val CHAIN_ID = BuildConfig.CHAIN_ID.toBigInteger()


