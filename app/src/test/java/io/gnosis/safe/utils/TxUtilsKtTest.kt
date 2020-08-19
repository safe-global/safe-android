package io.gnosis.safe.utils

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ParamsDto
import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.models.TransactionInfo
import io.gnosis.data.models.TransferInfo
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ADD_OWNER_WITH_THRESHOLD
import junit.framework.Assert.assertEquals
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class TxUtilsKtTest {

    @Test
    fun `formattedAmount (Unknown txInfo) should return 0 ETH`() {

        val txInfo = TransactionInfo.Unknown
        val result = txInfo.formattedAmount()

        assertEquals("0 ETH", result)
    }

    @Test
    fun `formattedAmount (Custom txInfo) should return 0 ETH`() {

        val txInfo = TransactionInfo.Custom(
            to = Solidity.Address(BigInteger.ZERO),
            dataSize = 0,
            value = BigInteger.ZERO
        )
        val result = txInfo.formattedAmount()

        assertEquals("0 ETH", result)
    }

    @Test
    fun `formattedAmount (Outgoing Transfer 1 ETH) should return -1 ETH`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.formattedAmount()

        assertEquals("-1 ETH", result)
    }

    @Test
    fun `formattedAmount (Outgoing Transfer 0 ETH) should return 0 ETH`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = BigInteger.ZERO)
        )
        val result = txInfo.formattedAmount()

        assertEquals("0 ETH", result)
    }

    @Test
    fun `formattedAmount (Incoming WETH Transfer) should return +0_1 WETH`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.INCOMING,
            transferInfo = buildErc20TransferInfo(value = BigInteger.ZERO)
        )
        val result = txInfo.formattedAmount()

        assertEquals("+0.1 WETH", result)
    }

    @Test
    fun `logoUri (Ether transfer) load ethereum url`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildTransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.logoUri()

        assertEquals("local::ethereum", result)
    }


    @Test
    fun `logoUri (WETH transfer) load ethereum url`() {

        val txInfo = TransactionInfo.Transfer(
            sender = Solidity.Address(BigInteger.ZERO),
            recipient = Solidity.Address(BigInteger.ONE),
            direction = TransactionDirection.OUTGOING,
            transferInfo = buildErc20TransferInfo(value = "1000000000000000000".toBigInteger())
        )
        val result = txInfo.logoUri()

        assertEquals("dummy", result)
    }


    @Test
    fun `txActionInfoItems () result `() {
        val settingsChange: TransactionInfo.SettingsChange =
            TransactionInfo.SettingsChange(
                dataDecoded = DataDecodedDto(
                    method = METHOD_ADD_OWNER_WITH_THRESHOLD,
                    parameters = listOf(ParamsDto("_threshold", "", "1"), ParamsDto("owner", "address", "0x1234"))
                )
            )

        val result = settingsChange.txActionInfoItems()

        // TODO WIP
        println("result: $result")
        //assertEquals("")

    }


    private fun buildTransferInfo(value: BigInteger): TransferInfo = TransferInfo.EtherTransfer(value)
    private fun buildErc20TransferInfo(value: BigInteger): TransferInfo =
        TransferInfo.Erc20Transfer(
            tokenAddress = Solidity.Address(BigInteger.ONE),
            value = BigInteger.ONE,
            logoUri = "dummy",
            decimals = 1,
            tokenName = "",
            tokenSymbol = "WETH"
        )
}
