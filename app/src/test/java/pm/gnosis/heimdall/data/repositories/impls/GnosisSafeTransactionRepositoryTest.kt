package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class GnosisSafeTransactionRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var accountRepositoryMock: AccountsRepository

    @Mock
    lateinit var ethereumJsonRpcRepositoryMock: EthereumJsonRpcRepository

    @Mock
    lateinit var descriptionsDaoMock: DescriptionsDao

    @Mock
    lateinit var appDbMock: ApplicationDb

    private lateinit var repository: GnosisSafeTransactionRepository

    @Before
    fun setUp() {
        given(appDbMock.descriptionsDao()).willReturn(descriptionsDaoMock)
        repository = GnosisSafeTransactionRepository(appDbMock, accountRepositoryMock, ethereumJsonRpcRepositoryMock)
    }

    private fun verifyHash(safe: BigInteger, transaction: Transaction, expected: String) {
        val observer = TestObserver<ByteArray>()
        repository.calculateHash(safe, transaction).subscribe(observer)
        observer.assertNoErrors().assertValueCount(1)
        assertEquals(expected, observer.values()[0].toHexString())
    }

    @Test
    fun calculateHash() {
        // Empty transaction
        verifyHash("0xbbf289d846208c16edc8474705c748aff07732db".hexAsBigInteger(),
                Transaction(BigInteger.ZERO, nonce = BigInteger.ZERO),
                "590bde81024e8282c3fb14e96309bd8e909637f271587eb201da7d18cf71d8f0")

        // Ether transfer
        verifyHash("0xbbf289d846208c16edc8474705c748aff07732db".hexAsBigInteger(),
                Transaction(
                        "0xa5056c8efadb5d6a1a6eb0176615692b6e648313".hexAsBigInteger(),
                        value = Wei(BigInteger("9223372036854775809")),
                        nonce = BigInteger("1337")
                ),
                "6749f074761471b989596fffa4dafc7b8a7d931d9881e8e1c4f3af46d8adb86a")

        // Token transfer
        val target = Solidity.Address("0xa5056c8efadb5d6a1a6eb0176615692b6e648313".hexAsBigInteger())
        val value = Solidity.UInt256(BigInteger("9223372036854775808"))
        val data = StandardToken.Transfer.encode(target, value)
        val transaction = Transaction(
                "0x9bebe3b9e7a461e35775ec935336891edf856da2".hexAsBigInteger(),
                data = data,
                nonce = BigInteger("7331")
        )
        verifyHash("0x09e1a843dfb9d1ae06c76a0c1e5c9a2b409cd9e4".hexAsBigInteger(), transaction,
                "31b20c3b6a3960aaf65808b18ec6d492d2649f0c8dae463bfe001c95263d9447")
    }

}