package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.db.daos.GnosisSafeDao
import pm.gnosis.heimdall.data.db.models.GnosisSafeInfoDb
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class DefaultGnosisSafeRepositoryTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var repository: DefaultGnosisSafeRepository

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var dbMock: ApplicationDb

    @Mock
    private lateinit var safeDaoMock: GnosisSafeDao

    @Mock
    private lateinit var descriptionsDaoMock: DescriptionsDao

    @Mock
    private lateinit var accountsRepository: AccountsRepository

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var pushRepositoryMock: PushServiceRepository

    @Captor
    private lateinit var safeInfoCaptor: ArgumentCaptor<GnosisSafeInfoDb>


    @Before
    fun setUp() {
        given(dbMock.gnosisSafeDao()).willReturn(safeDaoMock)
        given(dbMock.descriptionsDao()).willReturn(descriptionsDaoMock)
        repository = DefaultGnosisSafeRepository(
            contextMock,
            dbMock,
            accountsRepository,
            addressBookRepository,
            ethereumRepositoryMock,
            pushRepositoryMock,
            tokenRepositoryMock
        )
    }

    @Test
    fun checkSafeInvalidMasterCopyThresholdTooLow() {
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            val ethGetStorageAt = requests[0] as EthGetStorageAt
            assertEquals(TEST_SAFE, ethGetStorageAt.from)
            assertEquals(BigInteger.ZERO, ethGetStorageAt.location)
            assertEquals(Block.PENDING, ethGetStorageAt.block)
            ethGetStorageAt.response = EthRequest.Response.Success("0xdeadbeef".asEthereumAddress()!!.encode())

            (requests[1] as EthCall).response = EthRequest.Response.Success(Solidity.UInt256(BigInteger.ONE).encode())
            Observable.just(bulk)
        }
        val observer = TestObserver<Pair<Boolean, Boolean>>()
        repository.checkSafe(TEST_SAFE).subscribe(observer)
        observer.assertResult(false to false)
        then(ethereumRepositoryMock).should().request(MockUtils.any<BulkRequest>())
    }

    @Test
    fun checkSafeMasterCopyFailure() {
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            val ethGetStorageAt = requests[0] as EthGetStorageAt
            assertEquals(TEST_SAFE, ethGetStorageAt.from)
            assertEquals(BigInteger.ZERO, ethGetStorageAt.location)
            assertEquals(Block.PENDING, ethGetStorageAt.block)
            ethGetStorageAt.response = EthRequest.Response.Failure("Unknown address")
            (requests[1] as EthCall).response = EthRequest.Response.Success(Solidity.UInt256(BigInteger.ONE).encode())
            Observable.just(bulk)
        }
        val observer = TestObserver<Pair<Boolean, Boolean>>()
        repository.checkSafe(TEST_SAFE).subscribe(observer)
        observer.assertResult(false to false)
        then(ethereumRepositoryMock).should().request(MockUtils.any<BulkRequest>())
    }

    @Test
    fun checkSafeThresholdFailure() {
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            val ethGetStorageAt = requests[0] as EthGetStorageAt
            assertEquals(TEST_SAFE, ethGetStorageAt.from)
            assertEquals(BigInteger.ZERO, ethGetStorageAt.location)
            assertEquals(Block.PENDING, ethGetStorageAt.block)
            ethGetStorageAt.response = EthRequest.Response.Success("0xdeadbeef".asEthereumAddress()!!.encode())
            (requests[1] as EthCall).response = EthRequest.Response.Failure("EVM error")
            Observable.just(bulk)
        }
        val observer = TestObserver<Pair<Boolean, Boolean>>()
        repository.checkSafe(TEST_SAFE).subscribe(observer)
        observer.assertResult(false to false)
        then(ethereumRepositoryMock).should().request(MockUtils.any<BulkRequest>())
    }

    private fun testMasterCopy(masterCopy: String, threshold: Int) {
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            val ethGetStorageAt = requests[0] as EthGetStorageAt
            assertEquals(TEST_SAFE, ethGetStorageAt.from)
            assertEquals(BigInteger.ZERO, ethGetStorageAt.location)
            assertEquals(Block.PENDING, ethGetStorageAt.block)
            ethGetStorageAt.response = EthRequest.Response.Success(masterCopy.asEthereumAddress()!!.encode())
                (requests[1] as EthCall).response = EthRequest.Response.Success(Solidity.UInt256(BigInteger.valueOf(threshold.toLong())).encode())
            Observable.just(bulk)
        }
        val observer = TestObserver<Pair<Boolean, Boolean>>()
        repository.checkSafe(TEST_SAFE).subscribe(observer)
        observer.assertResult(true to true)
        then(ethereumRepositoryMock).should().request(MockUtils.any<BulkRequest>())
        Mockito.reset(ethereumRepositoryMock)
    }

    @Test
    fun checkSafe() {
        val masterCopyAddresses = BuildConfig.SUPPORTED_SAFE_MASTER_COPY_ADDRESSES.split(",") + BuildConfig.CURRENT_SAFE_MASTER_COPY_ADDRESS
        masterCopyAddresses.forEachIndexed { index, address -> testMasterCopy(address, index + 2) }
    }

    @Test
    fun saveOwner() {
        val pk = EncryptedByteArray.Converter().fromStorage("crypt_data")
        val address = "0xfeeddad0".asEthereumAddress()!!
        given(accountsRepository.saveOwner(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())
        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))

        val safeOwner = AccountsRepository.SafeOwner(address, pk)
        val testObserver = TestObserver<Unit>()
        repository.saveOwner(TEST_SAFE, safeOwner).subscribe(testObserver)

        testObserver.assertResult()
        then(accountsRepository).should().saveOwner(TEST_SAFE, safeOwner, ERC20Token.ETHER_TOKEN)
        then(accountsRepository).shouldHaveNoMoreInteractions()
        then(pushRepositoryMock).should().syncAuthentication(true)
        then(pushRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadPaymentToken(TEST_SAFE)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(safeDaoMock).shouldHaveZeroInteractions()
    }

    fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

    companion object {
        private val TEST_SAFE = "0xdeadfeedbeaf".asEthereumAddress()!!
    }
}
