package pm.gnosis.heimdall.data.remote.models.push

import org.junit.Assert.assertEquals
import org.junit.Test

class PushMessageTest {

    @Test(expected = IllegalArgumentException::class)
    fun throwMissingKey() {
        PushMessage.fromMap(mapOf("type" to "sendTransaction"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwUnknownType() {
        PushMessage.fromMap(mapOf("type" to "somerandomtype"))
    }

    @Test
    fun testFromMap() {
        PushMessage::class.nestedClasses.filter { it != PushMessage.Companion::class }.forEach {
            TEST_DATA[it]?.let {
                assertEquals(it.expected, PushMessage.fromMap(it.input))
            } ?: throw IllegalStateException("Missing test for ${it.simpleName}")
        }
    }

    data class TestData(val input: Map<String, String>, val expected: PushMessage)

    companion object {
        private const val TEST_R = "signature-r"
        private const val TEST_S = "signature-s"
        private const val TEST_V = "signature-v"
        private const val TEST_HASH = "somehash"
        private const val TEST_SAFE = "somehash"
        private const val TEST_TX_TO = "tx-to"
        private const val TEST_TX_VALUE = "tx-value"
        private const val TEST_TX_DATA = "tx-data"
        private const val TEST_TX_OPERATION = "tx-operation"
        private const val TEST_TX_GAS = "tx-gas"
        private const val TEST_OPERATIONAL_GAS = "operational-gas"
        private const val TEST_DATA_GAS = "gasfordata"
        private const val TEST_GAS_PRICE = "thegasprice"
        private const val TEST_GAS_TOKEN = "somegastoken"
        private const val TEST_NONCE = "somenonce"
        private val TEST_DATA = mapOf(
            PushMessage.RejectTransaction::class to
                    TestData(
                        mapOf(
                            "type" to "rejectTransaction",
                            "hash" to TEST_HASH,
                            "r" to TEST_R,
                            "s" to TEST_S,
                            "v" to TEST_V
                        ),
                        PushMessage.RejectTransaction(TEST_HASH, TEST_R, TEST_S, TEST_V)
                    ),

            PushMessage.ConfirmTransaction::class to
                    TestData(
                        mapOf(
                            "type" to "confirmTransaction",
                            "hash" to TEST_HASH,
                            "r" to TEST_R,
                            "s" to TEST_S,
                            "v" to TEST_V
                        ),
                        PushMessage.ConfirmTransaction(TEST_HASH, TEST_R, TEST_S, TEST_V)
                    ),

            PushMessage.SendTransaction::class to
                    TestData(
                        mapOf(
                            "type" to "sendTransaction",
                            "hash" to TEST_HASH,
                            "safe" to TEST_SAFE,
                            "to" to TEST_TX_TO,
                            "value" to TEST_TX_VALUE,
                            "data" to TEST_TX_DATA,
                            "operation" to TEST_TX_OPERATION,
                            "txGas" to TEST_TX_GAS,
                            "dataGas" to TEST_DATA_GAS,
                            "operationalGas" to TEST_OPERATIONAL_GAS,
                            "gasPrice" to TEST_GAS_PRICE,
                            "gasToken" to TEST_GAS_TOKEN,
                            "nonce" to TEST_NONCE,
                            "r" to TEST_R,
                            "s" to TEST_S,
                            "v" to TEST_V
                        ),
                        PushMessage.SendTransaction(
                            TEST_HASH,
                            TEST_SAFE,
                            TEST_TX_TO,
                            TEST_TX_VALUE,
                            TEST_TX_DATA,
                            TEST_TX_OPERATION,
                            TEST_TX_GAS,
                            TEST_DATA_GAS,
                            TEST_OPERATIONAL_GAS,
                            TEST_GAS_PRICE,
                            TEST_GAS_TOKEN,
                            TEST_NONCE,
                            TEST_R,
                            TEST_S,
                            TEST_V
                        )
                    )
        )
    }
}
