package io.gnosis.safe.notifications


import io.gnosis.safe.notifications.models.PushNotification
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress

class PushNotificationTest {

    @Test(expected = IllegalArgumentException::class)
    fun throwMissingKey() {
        PushNotification.fromMap(mapOf("type" to "EXECUTED_MULTISIG_TRANSACTION"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwUnknownType() {
        PushNotification.fromMap(mapOf("type" to "somerandomtype"))
    }

    @Test
    fun testFromMap() {
        PushNotification::class.nestedClasses.filter { it != PushNotification.Companion::class }.forEach {
            TEST_DATA[it]?.let {
                assertEquals(it.expected, PushNotification.fromMap(it.input))
            } ?: throw IllegalStateException("Missing test for ${it.simpleName}")
        }
    }

    data class TestData(val input: Map<String, String>, val expected: PushNotification)

    companion object {
        private const val TEST_SAFE = "0x0"
        private const val TEST_TX_HASH = "some hash"
        private const val TEST_SAFE_TX_HASH = "some tx hash"
        private const val TEST_VALUE = "1"
        private const val TEST_OWNER = "0x0"
        private const val TEST_FAILED = "false"
        private const val TEST_TOKEN_ADDRESS = "0x0"
        private const val TEST_TOKEN_ID = "tokenId"

        private val TEST_DATA = mapOf(
            PushNotification.ExecutedTransaction::class to
                    TestData(
                        mapOf(
                            "type" to "EXECUTED_MULTISIG_TRANSACTION",
                            "address" to TEST_SAFE,
                            "safeTxHash" to TEST_SAFE_TX_HASH,
                            "failed" to TEST_FAILED
                        ),
                        PushNotification.ExecutedTransaction(TEST_SAFE.asEthereumAddress()!!, TEST_SAFE_TX_HASH, TEST_FAILED.toBoolean())
                    ),
            //ERC20
            PushNotification.IncomingToken::class to
                    TestData(
                        mapOf(
                            "type" to "INCOMING_TOKEN",
                            "address" to TEST_SAFE,
                            "txHash" to TEST_TX_HASH,
                            "tokenAddress" to TEST_TOKEN_ADDRESS,
                            "value" to TEST_VALUE
                        ),
                        PushNotification.IncomingToken(
                            TEST_SAFE.asEthereumAddress()!!,
                            TEST_TX_HASH,
                            TEST_TOKEN_ADDRESS.asEthereumAddress()!!,
                            TEST_VALUE.toBigInteger()
                        )
                    ),
            //ERC721
            PushNotification.IncomingToken::class to
                    TestData(
                        mapOf(
                            "type" to "INCOMING_TOKEN",
                            "address" to TEST_SAFE,
                            "txHash" to TEST_TX_HASH,
                            "tokenAddress" to TEST_TOKEN_ADDRESS,
                            "tokenId" to TEST_TOKEN_ID
                        ),
                        PushNotification.IncomingToken(
                            TEST_SAFE.asEthereumAddress()!!,
                            TEST_TX_HASH,
                            TEST_TOKEN_ADDRESS.asEthereumAddress()!!,
                            null,
                            TEST_TOKEN_ID
                        )
                    ),
            PushNotification.IncomingEther::class to
                    TestData(
                        mapOf(
                            "type" to "INCOMING_ETHER",
                            "address" to TEST_SAFE,
                            "value" to TEST_VALUE,
                            "txHash" to TEST_TX_HASH
                        ),
                        PushNotification.IncomingEther(TEST_SAFE.asEthereumAddress()!!, TEST_TX_HASH, TEST_VALUE.toBigInteger())
                    )
        )
    }
}
