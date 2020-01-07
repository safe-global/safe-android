package pm.gnosis.heimdall.ui.transactions.builder

import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexStringToByteArray
import java.math.BigInteger

class TransactionBuilderTest {

    @Test
    fun testGenericTransactionBuilder() {
        assertEquals(
            SafeTransaction(Transaction(TEST_ADDRESS, value = Wei.ether("3.1415926")), TransactionExecutionRepository.Operation.CALL),
            GenericTransactionBuilder.build(TransactionData.Generic(TEST_ADDRESS, Wei.ether("3.1415926").value, null))
        )
        assertEquals(
            SafeTransaction(Transaction(TEST_ADDRESS, value = Wei.ZERO, data = "0xfdacd576"), TransactionExecutionRepository.Operation.CALL),
            GenericTransactionBuilder.build(TransactionData.Generic(TEST_ADDRESS, BigInteger.ZERO, "0xfdacd576"))
        )
    }

    @Test
    fun testAssetTransferTransactionBuilder() {
        assertEquals(
            SafeTransaction(Transaction(TEST_ADDRESS, value = Wei.ether("3.1415926")), TransactionExecutionRepository.Operation.CALL),
            AssetTransferTransactionBuilder.build(TransactionData.AssetTransfer(ETHER_TOKEN, Wei.ether("3.1415926").value, TEST_ADDRESS))
        )
        assertEquals(
            SafeTransaction(
                Transaction(
                    TEST_TOKEN,
                    data = "0xa9059cbb${TEST_ADDRESS.value.toString(16).padStart(64, '0')}${BigInteger("421000000").toString(16).padStart(64, '0')}"
                ), TransactionExecutionRepository.Operation.CALL
            ),
            AssetTransferTransactionBuilder.build(TransactionData.AssetTransfer(TEST_TOKEN, BigInteger("421000000"), TEST_ADDRESS))
        )
    }

    @Test
    fun testUpdateMasterCopyTransactionBuilder() {
        assertEquals(
            SafeTransaction(
                Transaction(
                    MULTI_SEND_ADDRESS, data = MultiSend.MultiSend.encode(
                        Solidity.Bytes(
                            ("" +
                                    // Update master copy
                                    "00" + // Operation
                                    "a7e15e2e76ab469f8681b576cff168f37aa246ec" + // To
                                    "0000000000000000000000000000000000000000000000000000000000000000" + // Value
                                    "0000000000000000000000000000000000000000000000000000000000000024" + // Data length
                                    "7de7edef" +
                                    "00000000000000000000000034CfAC646f301356fAa8B21e94227e3583Fe3F5F" +
                                    // Set fallback handler
                                    "00" + // Operation
                                    "a7e15e2e76ab469f8681b576cff168f37aa246ec" + // To
                                    "0000000000000000000000000000000000000000000000000000000000000000" + // Value
                                    "0000000000000000000000000000000000000000000000000000000000000024" + // Data length
                                    "f08a0323" +
                                    "000000000000000000000000d5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44"
                                    ).hexStringToByteArray()
                        )
                    )
                ),
                TransactionExecutionRepository.Operation.DELEGATE_CALL
            ),
            UpdateMasterCopyTransactionBuilder.build(TEST_TOKEN, TransactionData.UpdateMasterCopy(SafeContractUtils.safeMasterCopy_1_1_1))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUpdateMasterCopyTransactionBuilderInvalidContract() {
        UpdateMasterCopyTransactionBuilder.build(TEST_TOKEN, TransactionData.UpdateMasterCopy(TEST_ADDRESS))
    }

    @Test
    fun testMultiSendTransactionBuilder() {
        assertEquals(
            SafeTransaction(
                Transaction(MULTI_SEND_ADDRESS, data = MultiSend.MultiSend.encode(Solidity.Bytes(byteArrayOf()))),
                TransactionExecutionRepository.Operation.DELEGATE_CALL
            ),
            MultiSendTransactionBuilder.build(TransactionData.MultiSend(emptyList(), MULTI_SEND_ADDRESS))
        )

        assertEquals(
            SafeTransaction(
                Transaction(
                    MULTI_SEND_ADDRESS, data = MultiSend.MultiSend.encode(
                        Solidity.Bytes(
                            ("" +
                                    // Tx 1
                                    "00" + // Operation
                                    "a7e15e2e76ab469f8681b576cff168f37aa246ec" + // To
                                    "0000000000000000000000000000000000000000000000000000000000000000" + // Value
                                    "0000000000000000000000000000000000000000000000000000000000000024" + // Data length
                                    "7de7edef" +
                                    "000000000000000000000000c257274276a4e539741ca11b590b9447b26a8051" +
                                    // Tx 2
                                    "00" + // Operation
                                    "c257274276a4e539741ca11b590b9447b26a8051" + // To
                                    "0000000000000000000000000000000000000000000000000000000000000010" + // Value
                                    "0000000000000000000000000000000000000000000000000000000000000000" + // Data length
                                    // Tx 3
                                    "01" + // Operation
                                    "8D29bE29923b68abfDD21e541b9374737B49cdAD" + // To
                                    "0000000000000000000000000000000000000000000000000000000000000000" + // Value
                                    "0000000000000000000000000000000000000000000000000000000000000004" + // Data length
                                    "deadbeef"
                                    ).hexStringToByteArray()
                        )
                    )
                ),
                TransactionExecutionRepository.Operation.DELEGATE_CALL
            ),
            MultiSendTransactionBuilder.build(
                TransactionData.MultiSend(
                    listOf(
                        SafeTransaction(
                            Transaction(TEST_TOKEN, data = "0x7de7edef${TEST_ADDRESS.value.toString(16).padStart(64, '0')}"),
                            TransactionExecutionRepository.Operation.CALL
                        ),
                        SafeTransaction(
                            Transaction(TEST_ADDRESS, value = Wei(BigInteger.valueOf(16))),
                            TransactionExecutionRepository.Operation.CALL
                        ),
                        SafeTransaction(
                            Transaction(MULTI_SEND_ADDRESS, data = "0xdeadbeef"),
                            TransactionExecutionRepository.Operation.DELEGATE_CALL
                        )
                    ),
                    MULTI_SEND_ADDRESS
                )
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMultiSendTransactionBuilderInvalidContract() {
        MultiSendTransactionBuilder.build(TransactionData.MultiSend(emptyList(), TEST_ADDRESS))
    }

    companion object {
        private val MULTI_SEND_ADDRESS = "0x8D29bE29923b68abfDD21e541b9374737B49cdAD".asEthereumAddress()!!
        private val TEST_ADDRESS = "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!
        private val ETHER_TOKEN = Solidity.Address(BigInteger.ZERO)
        private val TEST_TOKEN = "0xa7e15e2e76ab469f8681b576cff168f37aa246ec".asEthereumAddress()!!
    }

}
