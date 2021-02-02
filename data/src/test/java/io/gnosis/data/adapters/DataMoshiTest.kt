package io.gnosis.data.adapters

import com.squareup.moshi.Types
import io.gnosis.data.models.transaction.*
import io.gnosis.data.models.transaction.TransactionStatus.SUCCESS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.stream.Collectors

class DataMoshiTest {

    private val moshi = dataMoshi
    private val txAdapter = moshi.adapter(Transaction::class.java)
    private val paramAdapter = moshi.adapter<List<Param>>(Types.newParameterizedType(List::class.java, Param::class.java))

    @Test
    fun `fromJson (ERC20 Transfer) should return Transfer`() {
        val jsonString: String = readResource("transfer_erc20.json")

        val transactionDto = txAdapter.fromJson(jsonString)

        val expected = Transaction(
            id = "ethereum_0x1C8b9B78e3085866521FE206fa4c1a67F49f153A_6613439_0xa215ac5bc96fb65e",
            timestamp = Date(1591357055000),
            txStatus = SUCCESS,
            txInfo = TransactionInfo.Transfer(
                direction = TransactionDirection.INCOMING,
                recipient = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!,
                recipientInfo = AddressInfo("Foo Bar Recipient", "https://www.foobar.de"),
                sender = "0x2134Bb3DE97813678daC21575E7A77a95079FC51".asEthereumAddress()!!,
                senderInfo = AddressInfo("Foo Bar Sender", "https://www.barfoo.de"),
                transferInfo = TransferInfo.Erc20Transfer(
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

        val transactionDto = txAdapter.fromJson(jsonString)

        val expected = Transaction(
            id = "multisig_0xa9b819d0123858cb3b37a849a2ed3d5c4341bd5ecb55bc7c2983301dc8d3cb5c",
            timestamp = Date(1591356920000),
            txStatus = SUCCESS,
            txInfo = TransactionInfo.Custom(
                dataSize = 0,
                to = "0x2134Bb3DE97813678daC21575E7A77a95079FC51".asEthereumAddress()!!,
                toInfo = AddressInfo("Foo Bar", "https://www.bar.de"),
                value = "3140000000000000000".toBigInteger(),
                methodName = null
            ),
            executionInfo = ExecutionInfo(
                nonce = 8.toBigInteger(),
                confirmationsRequired = 2,
                confirmationsSubmitted = 2,
                missingSigners = null
            )
        )
        assertEquals(expected, transactionDto)
    }

    @Test
    fun `fromJson (settings change tx) should return SettingsChange`() {
        val jsonString: String = readResource("settings_change.json")

        val transactionDto = txAdapter.fromJson(jsonString)

        val expected = Transaction(
            id = "multisig_0x238f1fe1beac3cfd4ec98acb4e006f7ebbbd615ddac9d6b065fdb3ce01065a9a",
            timestamp = Date(1591137785000),
            txStatus = SUCCESS,
            txInfo = TransactionInfo.SettingsChange(
                dataDecoded = DataDecoded(
                    method = "addOwnerWithThreshold",
                    parameters = listOf(
                        Param.Address(
                            name = "owner",
                            value = "0x5c9E7b93900536D9cc5559b881375Bae93c933D0".asEthereumAddress()!!,
                            type = "address"
                        ),
                        Param.Value(name = "_threshold", value = "1", type = "uint256")
                    )
                ),
                settingsInfo = SettingsInfo.AddOwner(
                    owner = "0x5c9E7b93900536D9cc5559b881375Bae93c933D0".asEthereumAddress()!!,
                    threshold = 1
                )
            ),
            executionInfo = ExecutionInfo(
                nonce = 4.toBigInteger(),
                confirmationsRequired = 1,
                confirmationsSubmitted = 1,
                missingSigners = null
            )
        )
        assertEquals(expected, transactionDto)
    }

    @Test
    fun `fromJson (creation TX) should not crash on certain null values `() {
        val jsonString: String = readResource("creation_with_null_implementation.json")

        val transactionDto = txAdapter.fromJson(jsonString)

        val expected = Transaction(
            id = "creation_0xFd0f138c1ca4741Ad6D4DecCb352Eb5c1E29D351",
            timestamp = Date(1543431151000),
            txStatus = SUCCESS,
            txInfo = TransactionInfo.Creation(
                creator = "0x40682efa0a7359ca4878AA87D1C010185c8b8d23".asEthereumAddress()!!,
                factory = null,
                implementation = null,
                transactionHash = "0xe4f00086442eadbac4975c6aa87110ac72700dca905da1d908947e04ed9cee47"
            ),
            executionInfo = null
        )
        assertEquals(expected, transactionDto)
    }

    @Test
    fun `fromJson (param list) - should map to correct param types`() {
        val jsonString: String = readResource("params.json")

        val params = paramAdapter.fromJson(jsonString)!!

        assertEquals(8, params?.size)

        assertTrue(params[0] is Param.Address)
        assertEquals("from", params[0].name)
        assertEquals("address", params[0].type)
        assertEquals("0x1230B3d59858296A31053C1b8562Ecf89A2f888b".asEthereumAddress(), params[0].value)

        assertTrue(params[1] is Param.Address)
        assertEquals("to", params[1].name)
        assertEquals("address", params[1].type)
        assertEquals("0x938bae50a210b80EA233112800Cd5Bc2e7644300".asEthereumAddress(), params[1].value)

        assertTrue(params[2] is Param.Array)
        assertEquals("ids", params[2].name)
        assertEquals("uint256[]", params[2].type)
        assertEquals(listOf("1000"), params[2].value)

        assertTrue(params[3] is Param.Array)
        assertEquals("values", params[3].name)
        assertEquals("uint256[][]", params[3].type)
        assertEquals(listOf(listOf("100"), listOf("100000", "2000")), params[3].value)

        assertTrue(params[4] is Param.Bytes)
        assertEquals("data", params[4].name)
        assertEquals("bytes", params[4].type)
        assertEquals("0x00", params[4].value)

        assertTrue(params[5] is Param.Bytes)
        assertEquals("transactions", params[5].name)
        assertEquals("bytes", params[5].type)
        assertEquals(
            "0x00c778417e063141139fce010982780140aa0cd5ab00000000000000000000000000000000000000000000000000470de4df81ffec0000000000000000000000000000000000000000000000000000000000000004d0e30db000c778417e063141139fce010982780140aa0cd5ab00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000044095ea7b30000000000000000000000007ee114c3628ca90119fc699f03665bf9db8f5faf00000000000000000000000000000000000000000000000000470de4df81ffec007ee114c3628ca90119fc699f03665bf9db8f5faf000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a4dc7d93690000000000000000000000006e45d69a383ceca3d54688e833bd0e1388747e6b00000000000000000000000000000000000000000000000000470de4df81ffec000000000000000000000000c778417e063141139fce010982780140aa0cd5ab000000000000000000000000000000000000000000000000000000005f59386c000000000000000000000000000000000000000000000000000000005f593920",
            params[5].value
        )

        assertTrue(params[6] is Param.Bytes)
        assertEquals("data", params[6].name)
        assertEquals("bytes", params[6].type)
        assertEquals("0x00", params[6].value)

        assertTrue(params[7] is Param.Bytes)
        assertEquals("data", params[7].name)
        assertEquals("bytes", params[7].type)
        assertEquals("0x00", params[7].value)

        val json = paramAdapter.toJson(params)
        assertTrue(json.isNotEmpty())
    }

    @Test
    fun `fromJson (empty param list) - should handle empty list`() {
        val jsonString = "[]"
        val params = paramAdapter.fromJson(jsonString)!!
        assertEquals(0, params.size)
    }

    private fun readResource(fileName: String): String {
        return BufferedReader(
            InputStreamReader(
                this::class.java.getClassLoader()?.getResourceAsStream(fileName)!!
            )
        ).lines().parallel().collect(Collectors.joining("\n"))
    }
}
