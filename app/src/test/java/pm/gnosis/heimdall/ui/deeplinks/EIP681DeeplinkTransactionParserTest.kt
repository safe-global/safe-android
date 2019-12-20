package pm.gnosis.heimdall.ui.deeplinks

import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import java.math.BigInteger

class EIP681DeeplinkTransactionParserTest {
    private val parser = EIP681DeeplinkTransactionParser()

    @Test
    fun parseValueTransfer() {
        assertEquals(
            parser.parse("ethereum:${TEST_SAFE.asEthereumAddressChecksumString()}@${BuildConfig.BLOCKCHAIN_CHAIN_ID}?value=42"),
            SafeTransaction(
                Transaction(
                    TEST_SAFE,
                    value = Wei(BigInteger.valueOf(42))
                ),
                TransactionExecutionRepository.Operation.CALL
            ) to null
        )
        assertEquals(
            parser.parse("ethereum://${TEST_SAFE.asEthereumAddressChecksumString()}?value=42"),
            SafeTransaction(
                Transaction(
                    TEST_SAFE,
                    value = Wei(BigInteger.valueOf(42))
                ),
                TransactionExecutionRepository.Operation.CALL
            ) to null
        )
        assertEquals(
            parser.parse("ethereum:${TEST_SAFE.asEthereumAddressChecksumString()}?value=0x2a&gas=123&gasLimit=3543432&gasPrice=12323"),
            SafeTransaction(
                Transaction(
                    TEST_SAFE,
                    value = Wei(BigInteger.valueOf(42))
                ),
                TransactionExecutionRepository.Operation.CALL
            ) to null
        )
        assertEquals(
            parser.parse("ethereum:${TEST_SAFE.asEthereumAddressChecksumString()}?value=0x2a&referrer=some_referrer_url"),
            SafeTransaction(
                Transaction(
                    TEST_SAFE,
                    value = Wei(BigInteger.valueOf(42))
                ),
                TransactionExecutionRepository.Operation.CALL
            ) to "some_referrer_url"
        )
    }

    @Test(expected = IllegalStateException::class)
    fun throwWrongChainID() {
        parser.parse("ethereum:${TEST_SAFE.asEthereumAddressChecksumString()}@${BuildConfig.BLOCKCHAIN_CHAIN_ID + 1}?value=42")
    }

    @Test
    fun parseExecTransaction() {
        val transaction = SafeTransaction(
            Transaction(
                TEST_SAFE,
                value = null,
                data = GnosisSafe.ExecTransaction.encode(
                    TEST_ADDRESS,
                    Solidity.UInt256(BigInteger.valueOf(42)),
                    Solidity.Bytes("0x1db61b54".hexToByteArray()),
                    Solidity.UInt8(BigInteger.ZERO),
                    Solidity.UInt256(BigInteger.ZERO),
                    Solidity.UInt256(BigInteger.ZERO),
                    Solidity.UInt256(BigInteger.ZERO),
                    Solidity.Address(BigInteger.ZERO),
                    Solidity.Address(BigInteger.ZERO),
                    Solidity.Bytes("0x0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000001bde0b9f486b1960454e326375d0b1680243e031fd4fb3f070d9a3ef9871ccfd57d1a653cffb6321f889169f08e548684e005f2b0c3a6c06fba4c4a68f5e006241c".hexToByteArray())
                )
            ),
            TransactionExecutionRepository.Operation.CALL
        )
        val executeLink = "ethereum:${TEST_SAFE.asEthereumAddressString()}/execTransaction?" +
                "address=" + TEST_ADDRESS.asEthereumAddressChecksumString() +
                "&uint256=42" +
                "&bytes=0x1db61b54" +
                "&uint8=0" +
                "&uint256=0x0" +
                "&uint256=0x0" +
                "&uint256=0" +
                "&address=0x0" +
                "&address=" + Solidity.Address(BigInteger.ZERO).asEthereumAddressString() +
                "&bytes=0x0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000001bde0b9f486b1960454e326375d0b1680243e031fd4fb3f070d9a3ef9871ccfd57d1a653cffb6321f889169f08e548684e005f2b0c3a6c06fba4c4a68f5e006241c"
        assertEquals(
            transaction to null,
            parser.parse(executeLink)
        )
    }

    @Test
    fun parseBaseTypes() {
        val transaction = SafeTransaction(
            Transaction(
                TEST_SAFE,
                value = null,
                data = "0x18442b4d" + SolidityBase.encodeTuple(
                    listOf(
                        Solidity.Bool(true),
                        Solidity.Bool(false),
                        Solidity.Bool(true),
                        Solidity.UInt256(BigInteger.ONE),
                        Solidity.UInt256(BigInteger.valueOf(2)),
                        Solidity.Int256(BigInteger.valueOf(-2)),
                        Solidity.Int256(BigInteger.valueOf(-1)),
                        Solidity.Int256(BigInteger.TEN),
                        Solidity.String("this is some string"),
                        Solidity.String("hex string"),
                        Solidity.Bytes32("0xdeadbeef".hexToByteArray())
                    )
                )
            ),
            TransactionExecutionRepository.Operation.CALL
        )
        val link = "ethereum:${TEST_SAFE.asEthereumAddressString()}/baseTypes?" +
                "bool=true" +
                "&bool=false" +
                "&bool=0x01"+
                "&uint=0x01"+
                "&uint8=2"+
                "&int=-2" +
                "&int=0xff" +
                "&int=10" +
                "&string=this is some string" +
                "&string=" + "hex string".toByteArray().toHex().addHexPrefix() +
                "&bytes32=0xdeadbeef"
        assertEquals(
            transaction to null,
            parser.parse(link)
        )
    }

    companion object {
        private val TEST_ADDRESS = "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!
        private val TEST_SAFE = "0xa7e15e2e76ab469f8681b576cff168f37aa246ec".asEthereumAddress()!!
    }
}