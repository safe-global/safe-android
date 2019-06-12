package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.db.daos.GnosisSafeDao
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.security.EncryptionManager
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
    private lateinit var pushRepositoryMock: PushServiceRepository

    @Mock
    private lateinit var bip39: Bip39

    @Mock
    private lateinit var encryptionManager: EncryptionManager


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
            bip39,
            encryptionManager
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

    companion object {
        private val TEST_SAFE = "0xdeadfeedbeaf".asEthereumAddress()!!
    }
}
