package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.IpfsApi
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
    lateinit var ethereumJsonRpcRepositoryMock: EthereumJsonRpcRepository

    @Mock
    lateinit var descriptionsDaoMock: DescriptionsDao

    @Mock
    lateinit var appDbMock: ApplicationDb

    @Mock
    lateinit var ipfsApiMock: IpfsApi

    private lateinit var repository: SimpleTransactionDetailsRepository

    @Before
    fun setUp() {
        given(appDbMock.descriptionsDao()).willReturn(descriptionsDaoMock)
        repository = SimpleTransactionDetailsRepository(appDbMock)
    }

    private fun testLoadTransactionType(transaction: Transaction, expectedType: TransactionType) {
        val testObserver = TestObserver<TransactionType>()
        repository.loadTransactionType(transaction).subscribe(testObserver)
        testObserver.assertNoErrors().assertValue(expectedType).assertComplete()

        then(ethereumJsonRpcRepositoryMock).shouldHaveNoMoreInteractions()
        then(descriptionsDaoMock).shouldHaveNoMoreInteractions()
        then(ipfsApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionType() {
        // Empty transaction = Generic
        testLoadTransactionType(Transaction(BigInteger.ZERO), TransactionType.GENERIC)

        // Ether transfer
        testLoadTransactionType(Transaction(BigInteger.ZERO, value = Wei(BigInteger.TEN)), TransactionType.ETHER_TRANSFER)

        // Token transfer
        val tokenTransferData = StandardToken.Transfer.encode(Solidity.Address(BigInteger.ONE), Solidity.UInt256(BigInteger.TEN))
        testLoadTransactionType(Transaction(BigInteger.ZERO, data = tokenTransferData), TransactionType.TOKEN_TRANSFER)
    }

}