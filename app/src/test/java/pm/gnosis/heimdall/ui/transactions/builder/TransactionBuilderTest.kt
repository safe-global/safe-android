package pm.gnosis.heimdall.ui.transactions.builder

import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddress
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
                Transaction(TEST_TOKEN, data = "0x7de7edef${TEST_ADDRESS.value.toString(16).padStart(64, '0')}"),
                TransactionExecutionRepository.Operation.CALL
            ),
            UpdateMasterCopyTransactionBuilder.build(TEST_TOKEN, TransactionData.UpdateMasterCopy(TEST_ADDRESS))
        )
    }

    companion object {
        private val TEST_ADDRESS = "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!
        private val ETHER_TOKEN = Solidity.Address(BigInteger.ZERO)
        private val TEST_TOKEN = "0xa7e15e2e76ab469f8681b576cff168f37aa246ec".asEthereumAddress()!!
    }

}
