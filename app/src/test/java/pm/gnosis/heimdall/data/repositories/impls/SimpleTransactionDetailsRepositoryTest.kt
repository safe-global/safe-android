package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SimpleTransactionDetailsRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    lateinit var descriptionsDaoMock: DescriptionsDao

    @Mock
    lateinit var settingsRepositoryMock: SettingsRepository

    @Mock
    lateinit var appDbMock: ApplicationDb

    private lateinit var repository: SimpleTransactionDetailsRepository

    @Before
    fun setUp() {
        given(appDbMock.descriptionsDao()).willReturn(descriptionsDaoMock)
        repository = SimpleTransactionDetailsRepository(appDbMock, settingsRepositoryMock)
    }

    private fun testLoadTransactionType(transaction: Transaction, expectedType: TransactionType) {
        val testObserver = TestObserver<TransactionType>()
        repository.loadTransactionType(transaction).subscribe(testObserver)
        testObserver.assertNoErrors().assertValue(expectedType).assertComplete()

        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
        then(descriptionsDaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionType() {
        val transactionTypes = TransactionType.values().toMutableList()
        TESTS.forEach { (transaction, expectedType) ->
            testLoadTransactionType(transaction, expectedType)
            transactionTypes.remove(expectedType)
        }
        assertEquals("We should test all possible wrapped types", emptyList<TransactionType>(), transactionTypes)
    }

    companion object {
        private val TEST_OWNER = Solidity.Address(BigInteger.valueOf(13))
        private val TESTS = listOf(
            // Empty transaction = Ether transfer
            Transaction(Solidity.Address(BigInteger.ZERO)) to TransactionType.ETHER_TRANSFER,
            // Ether transfer
            Transaction(Solidity.Address(BigInteger.ZERO), value = Wei(BigInteger.TEN)) to TransactionType.ETHER_TRANSFER,
            // Token transfer
            Transaction(
                Solidity.Address(BigInteger.ZERO),
                data = StandardToken.Transfer.encode(Solidity.Address(BigInteger.ONE), Solidity.UInt256(BigInteger.TEN))
            ) to TransactionType.TOKEN_TRANSFER,
            // Add safe owners
            Transaction(
                Solidity.Address(BigInteger.ZERO),
                data = GnosisSafe.AddOwner.encode(TEST_OWNER, Solidity.UInt8(BigInteger.ONE))
            ) to TransactionType.ADD_SAFE_OWNER,
            // Remove safe owners
            Transaction(
                Solidity.Address(BigInteger.ZERO),
                data = GnosisSafe.RemoveOwner.encode(Solidity.UInt256(BigInteger.TEN), TEST_OWNER, Solidity.UInt8(BigInteger.ONE))
            ) to TransactionType.REMOVE_SAFE_OWNER,
            // Replace safe owners
            Transaction(
                Solidity.Address(BigInteger.ZERO),
                data = GnosisSafe.ReplaceOwner.encode(Solidity.UInt256(BigInteger.TEN), TEST_OWNER, Solidity.Address(BigInteger.TEN))
            ) to TransactionType.REPLACE_SAFE_OWNER,
            // Unknown data = Generic transaction
            Transaction(
                Solidity.Address(BigInteger.ZERO),
                data = GnosisSafe.Owners.encode(Solidity.UInt256(BigInteger.ZERO))
            ) to TransactionType.GENERIC
        )
    }
}
