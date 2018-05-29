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
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.asEthereumAddress
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
    lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    lateinit var descriptionsDaoMock: DescriptionsDao

    @Mock
    lateinit var relayServiceApiMock: RelayServiceApi

    @Mock
    lateinit var appDbMock: ApplicationDb

    private lateinit var repository: DefaultTransactionExecutionRepository

    @Before
    fun setUp() {
        given(appDbMock.descriptionsDao()).willReturn(descriptionsDaoMock)
        repository = DefaultTransactionExecutionRepository(appDbMock, accountRepositoryMock, ethereumRepositoryMock, relayServiceApiMock)
    }

    private fun verifyHash(
        safe: BigInteger,
        transaction: Transaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: BigInteger,
        operation: TransactionExecutionRepository.Operation,
        expected: String
    ) {
        val observer = TestObserver<ByteArray>()
        repository.calculateHash(
            Solidity.Address(safe), SafeTransaction(transaction, operation),
            txGas, dataGas, gasPrice, Solidity.Address(gasToken)
        ).subscribe(observer)
        observer.assertNoErrors().assertValueCount(1)
        assertEquals(expected, observer.values()[0].toHexString())
    }

    @Test
    fun calculateHash() {
        // Empty wrapped
        verifyHash(
            "0xbbf289d846208c16edc8474705c748aff07732db".hexAsBigInteger(),
            Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.ZERO),
            BigInteger("100000"), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
            TransactionExecutionRepository.Operation.CALL,
            "975734ee1e75ec3300bbb8bad5f79597a30f8b61fac48350822cf60492386f7f"
        )

        // Ether transfer
        verifyHash(
            "0xbbf289d846208c16edc8474705c748aff07732db".hexAsBigInteger(),
            Transaction(
                "0xa5056c8efadb5d6a1a6eb0176615692b6e648313".asEthereumAddress()!!,
                value = Wei(BigInteger("9223372036854775809")),
                nonce = BigInteger("1337")
            ),
            BigInteger("4200000"), BigInteger("13337"), BigInteger("20000000000"), BigInteger.ZERO,
            TransactionExecutionRepository.Operation.CALL,
            "7e4cb4cd190aedb510e8c4d366a87a8ee948921796ea7d720c74db3fc4be4db3"
        )

        // Token transfer
        val target = Solidity.Address("0xa5056c8efadb5d6a1a6eb0176615692b6e648313".hexAsBigInteger())
        val value = Solidity.UInt256(BigInteger("9223372036854775808"))
        val data = ERC20Contract.Transfer.encode(target, value)
        val transaction = Transaction(
            "0x9bebe3b9e7a461e35775ec935336891edf856da2".asEthereumAddress()!!,
            data = data,
            nonce = BigInteger("7331")
        )
        verifyHash(
            "0x09e1a843dfb9d1ae06c76a0c1e5c9a2b409cd9e4".hexAsBigInteger(), transaction,
            BigInteger("230000"), BigInteger("7331"), BigInteger("100"),
            "0xbbf289d846208c16edc8474705c748aff07732db".asEthereumAddress()!!.value,
            TransactionExecutionRepository.Operation.CALL,
            "1297208903774650b1a7fa09a60ffd69b4a253a010df54d201c0f500a4f80c46"
        )

        // Token transfer as delegate call
        verifyHash(
            "0x09e1a843dfb9d1ae06c76a0c1e5c9a2b409cd9e4".hexAsBigInteger(), transaction,
            BigInteger("230000"), BigInteger("7331"), BigInteger("100"),
            "0xbbf289d846208c16edc8474705c748aff07732db".asEthereumAddress()!!.value,
            TransactionExecutionRepository.Operation.DELEGATE_CALL,
            "c93063108c748057a30815731a7e0777acd4df20d189c22088d01571c21f7e32"
        )
    }
}
