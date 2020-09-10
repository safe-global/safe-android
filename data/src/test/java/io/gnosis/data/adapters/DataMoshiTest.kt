package io.gnosis.data.adapters

import io.gnosis.data.backend.dto.*
import io.gnosis.data.models.TransactionStatus.SUCCESS
import junit.framework.Assert.assertEquals
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class DataMoshiTest {
    private val moshi = dataMoshi
    private val adapter = moshi.adapter(GateTransactionDto::class.java)

    @Test
    fun `fromJson (ERC20 Transfer) should return Transfer`() {
        val jsonString: String = readResource("transfer_erc20.json")

        val transactionDto = adapter.fromJson(jsonString)

        val expected = GateTransactionDto(
            id = "ethereum_0x1C8b9B78e3085866521FE206fa4c1a67F49f153A_6613439_0xa215ac5bc96fb65e",
            timestamp = 1591357055000,
            txStatus = SUCCESS,
            txInfo = TransactionInfoDto.Transfer(
                type = GateTransactionType.Transfer,
                direction = TransactionDirection.INCOMING,
                recipient = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!,
                sender = "0x2134Bb3DE97813678daC21575E7A77a95079FC51".asEthereumAddress()!!,
                transferInfo = TransferInfoDto.Erc20Transfer(
                    type = GateTransferType.ERC20,
                    decimals = 18,
                    logoUri = "https://gnosis-safe-token-logos.s3.amazonaws.com/0xc778417E063141139Fce010982780140Aa0cD5Ab.png",
                    tokenAddress = "0xc778417E063141139Fce010982780140Aa0cD5Ab".asEthereumAddress()!!,
                    tokenName = "Wrapped Ether",
                    tokenSymbol = "WETH",
                    value = "1000000000000000000".toBigInteger()
                )
            ),
            executionInfo = null
        )
        assertEquals(expected, transactionDto)
    }

    @Test
    fun `fromJson (Custom tx) should return Custom`() {
        val jsonString: String = readResource("custom.json")

        val transactionDto = adapter.fromJson(jsonString)

        val expected = GateTransactionDto(
            id = "multisig_0xa9b819d0123858cb3b37a849a2ed3d5c4341bd5ecb55bc7c2983301dc8d3cb5c",
            timestamp = 1591356920000,
            txStatus = SUCCESS,
            txInfo = TransactionInfoDto.Custom(
                type = GateTransactionType.Custom,
                dataSize = 0,
                to = "0x2134Bb3DE97813678daC21575E7A77a95079FC51".asEthereumAddress()!!,
                value = "3140000000000000000".toBigInteger()
            ),
            executionInfo = ExecutionInfoDto(
                nonce = 8.toBigInteger(),
                confirmationsRequired = 2,
                confirmationsSubmitted = 2
            )
        )
        assertEquals(expected, transactionDto)
    }

    @Test
    fun `fromJson (settings change tx) should return SettingsChange`() {
        val jsonString: String = readResource("settings_change.json")

        val transactionDto = adapter.fromJson(jsonString)

        val expected = GateTransactionDto(
            id = "multisig_0x238f1fe1beac3cfd4ec98acb4e006f7ebbbd615ddac9d6b065fdb3ce01065a9a",
            timestamp = 1591137785000,
            txStatus = SUCCESS,
            txInfo = TransactionInfoDto.SettingsChange(
                type = GateTransactionType.SettingsChange,
                dataDecoded = DataDecodedDto(
                    method = "addOwnerWithThreshold",
                    parameters = listOf(
                        ParamsDto(name = "owner", value = "0x5c9E7b93900536D9cc5559b881375Bae93c933D0", type = "address"),
                        ParamsDto(name = "_threshold", value = "1", type = "uint256")
                    )
                )
            ),
            executionInfo = ExecutionInfoDto(
                nonce = 4.toBigInteger(),
                confirmationsRequired = 1,
                confirmationsSubmitted = 1
            )
        )
        assertEquals(expected, transactionDto)
    }

    @Test
    fun `fromJson (creation TX) should not crash on certain null values `() {
        val jsonString: String = readResource("creation_with_null_implementation.json")

        val transactionDto = adapter.fromJson(jsonString)

        val expected = GateTransactionDto(
            id = "creation_0xFd0f138c1ca4741Ad6D4DecCb352Eb5c1E29D351",
            timestamp = 1543431151000,
            txStatus = SUCCESS,
            txInfo = TransactionInfoDto.Creation(
                type = GateTransactionType.Creation,
                creator = "0x40682efa0a7359ca4878AA87D1C010185c8b8d23".asEthereumAddress()!!,
                factory = null,
                implementation = null,
                transactionHash = "0xe4f00086442eadbac4975c6aa87110ac72700dca905da1d908947e04ed9cee47"
            ),
            executionInfo = null
        )
        assertEquals(expected, transactionDto)
    }

    private fun readResource(fileName: String): String {
        return BufferedReader(
            InputStreamReader(
                this::class.java.getClassLoader()?.getResourceAsStream(fileName)!!
            )
        ).lines().parallel().collect(Collectors.joining("\n"))
    }
}
