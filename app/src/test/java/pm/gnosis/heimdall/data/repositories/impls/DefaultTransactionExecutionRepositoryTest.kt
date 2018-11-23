package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.BulkRequest
import pm.gnosis.ethereum.EthRequest
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.ExecuteParams
import pm.gnosis.heimdall.data.remote.models.RelayExecution
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.*
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class DefaultTransactionExecutionRepositoryTest {
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
    lateinit var pushServiceRepositoryMock: PushServiceRepository

    @Mock
    lateinit var relayServiceApiMock: RelayServiceApi

    @Mock
    lateinit var appDbMock: ApplicationDb

    private lateinit var repository: DefaultTransactionExecutionRepository

    @Before
    fun setUp() {
        given(appDbMock.descriptionsDao()).willReturn(descriptionsDaoMock)
        repository = DefaultTransactionExecutionRepository(
            appDbMock,
            accountRepositoryMock,
            ethereumRepositoryMock,
            pushServiceRepositoryMock,
            relayServiceApiMock
        )
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
        // Shared test cases: https://github.com/gnosis/gnosis-py/blob/master/gnosis/safe/tests/test_safe_service.py
        verifyHash(
            "0x692a70d2e424a56d2c6c27aa97d1a86395877b3a".hexAsBigInteger(),
            Transaction(
                "0x5AC255889882aaB35A2aa939679E3F3d4Cea221E".asEthereumAddress()!!,
                data = "0x00",
                value = Wei(BigInteger.valueOf(5000000)),
                nonce = BigInteger.valueOf(67)
            ),
            BigInteger("50000"), BigInteger("100"), BigInteger("10000"), BigInteger.ZERO,
            TransactionExecutionRepository.Operation.CALL,
            "c9d69a2350aede7978fdee58e702647e4bbdc82168577aa4a43b66ad815c6d1a"
        )

        verifyHash(
            "0x692a70d2e424a56d2c6c27aa97d1a86395877b3a".hexAsBigInteger(),
            Transaction(
                "0x0".asEthereumAddress()!!,
                value = Wei(BigInteger.valueOf(80000000)),
                data = "0x562944",
                nonce = BigInteger.valueOf(257000)
            ),
            BigInteger("54522"), BigInteger("773"), BigInteger("22000000"), BigInteger.ZERO,
            TransactionExecutionRepository.Operation.CREATE,
            "8ca8db91d72b379193f6e229eb2dff0d0621b6ef452d90638ee3206e9b7349b3"
        )

        // Empty wrapped
        verifyHash(
            "0xbbf289d846208c16edc8474705c748aff07732db".hexAsBigInteger(),
            Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.ZERO),
            BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
            TransactionExecutionRepository.Operation.CALL,
            "1863bbed0d8fdd3c5b132495bac41db35cf3a6190ccd02cd511199b9476d269e"
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
            "30ae451e4e933a6e7703221298a2baf5292b3a954b67e0ddef997b1754920db0"
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
            "d3bfdcdd807f4db717646a35f333ed90cab0f94f31a0d428f688429c41039fe4"
        )

        // Token transfer as delegate call
        verifyHash(
            "0x09e1a843dfb9d1ae06c76a0c1e5c9a2b409cd9e4".hexAsBigInteger(), transaction,
            BigInteger("230000"), BigInteger("7331"), BigInteger("100"),
            "0xbbf289d846208c16edc8474705c748aff07732db".asEthereumAddress()!!.value,
            TransactionExecutionRepository.Operation.DELEGATE_CALL,
            "e0c6798c794ee4f3a346f98b5be5225aec4b159e586271004b0a7b34653b0657"
        )
    }

    @Test
    fun submitTransactionNotOwner() {
        given(relayServiceApiMock.execute(MockUtils.any(), MockUtils.any())).willReturn(Single.just(RelayExecution(SERVICE_TX_HASH)))
        val tx = SafeTransaction(Transaction(TEST_ADDRESS, TEST_ETH_AMOUNT, nonce = BigInteger.TEN), TransactionExecutionRepository.Operation.CALL)
        val observer = TestObserver<String>()
        repository.submit(TEST_SAFE, tx, mapOf(TEST_OWNER to TEST_SIGNATURE), false, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
            ERC20Token.ETHER_TOKEN.address, false)
            .subscribe(observer)
        observer.assertResult(TEST_TX_HASH)
        then(relayServiceApiMock).should().execute(
            TEST_SAFE.asEthereumAddressChecksumString(),
            ExecuteParams(
                TEST_ADDRESS.asEthereumAddressChecksumString(),
                TEST_ETH_AMOUNT.value.asDecimalString(),
                null,
                0,
                listOf(ServiceSignature(BigInteger.TEN, BigInteger.TEN, 27)),
                "0",
                "0",
                "0",
                ERC20Token.ETHER_TOKEN.address.asEthereumAddressChecksumString(),
                10
            )
        )
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        then(accountRepositoryMock).shouldHaveZeroInteractions()

        // Check that nonce was cached
        given(accountRepositoryMock.loadActiveAccount()).willReturn(Single.just(Account(TEST_OWNER)))
        var remoteNonceString = "9".padStart(64, '0').addHexPrefix()
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).willAnswer {
            (it.arguments.first() as BulkRequest).let { bulk ->
                (bulk.requests[0] as EthRequest<String>).response = EthRequest.Response.Success("2".padStart(64, '0').addHexPrefix())
                (bulk.requests[1] as EthRequest<String>).response = EthRequest.Response.Success(remoteNonceString)
                (bulk.requests[2] as EthRequest<String>).response =
                        EthRequest.Response.Success("20".padStart(64, '0').padEnd(128, '0').addHexPrefix())
                (bulk.requests[3] as EthRequest<Wei>).response = EthRequest.Response.Success(Wei.ether("0"))
                Observable.just(bulk)
            }
        }

        // Cached nonce should be used if it is higher
        val observeCached = TestObserver<TransactionExecutionRepository.SafeExecuteState>()
        repository.loadSafeExecuteState(TEST_SAFE, TEST_PAYMENT_TOKEN).subscribe(observeCached)
        observeCached.assertResult(
            TransactionExecutionRepository.SafeExecuteState(
                TEST_OWNER,
                2,
                emptyList(),
                BigInteger.valueOf(11), // nonce used with submit + 1
                BigInteger.ZERO
            )
        )
        then(ethereumRepositoryMock).should().request(MockUtils.any<BulkRequest>())
        then(accountRepositoryMock).should().loadActiveAccount()

        // Remote nonce should be used if it is higher
        remoteNonceString = "1a".padStart(64, '0').addHexPrefix()
        val observeRemote = TestObserver<TransactionExecutionRepository.SafeExecuteState>()
        repository.loadSafeExecuteState(TEST_SAFE, TEST_PAYMENT_TOKEN).subscribe(observeRemote)
        observeRemote.assertResult(
            TransactionExecutionRepository.SafeExecuteState(
                TEST_OWNER,
                2,
                emptyList(),
                BigInteger.valueOf(26), // nonce used with submit + 1
                BigInteger.ZERO
            )
        )
        then(ethereumRepositoryMock).should(times(2)).request(MockUtils.any<BulkRequest>())
        then(accountRepositoryMock).should(times(2)).loadActiveAccount()

        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountRepositoryMock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        then(descriptionsDaoMock).shouldHaveZeroInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()
    }

    companion object {
        private const val SERVICE_TX_HASH = "dae721569a948b87c269ebacaa5a4a67728095e32f9e7e4626f109f27a73b40f"
        private val TEST_TX_HASH = SERVICE_TX_HASH.addHexPrefix()
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private val TEST_ADDRESS = "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!
        private val TEST_OWNER = Solidity.Address(BigInteger.valueOf(5))
        private val TEST_SIGNATURE = Signature(BigInteger.TEN, BigInteger.TEN, 27)
        private val TEST_ETH_AMOUNT = Wei.ether("23")
        private val TEST_PAYMENT_TOKEN = "0x0".asEthereumAddress()!!
    }
}
