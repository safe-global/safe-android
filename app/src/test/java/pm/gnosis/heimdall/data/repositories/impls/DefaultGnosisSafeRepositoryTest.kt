package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.*
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.db.daos.GnosisSafeDao
import pm.gnosis.heimdall.data.db.models.GnosisSafeInfoDb
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
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
    private lateinit var encryptionManagerMock: EncryptionManager

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
            bip39,
            encryptionManagerMock
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
        val owner = "0xfeeddad0".asEthereumAddress()!!
        val ownerKey = Sha3Utils.keccak("cow".toByteArray())
        val cryptoData = EncryptionManager.CryptoData("0bad".hexToByteArray(), "dad0".hexToByteArray())
        given(encryptionManagerMock.encrypt(MockUtils.any()))
            .willReturn(cryptoData)

        val testObserver = TestObserver<Unit>()
        repository.saveOwner(TEST_SAFE, owner, ownerKey).subscribe(testObserver)

        testObserver.assertResult()
        then(encryptionManagerMock).should().encrypt(ownerKey)
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(safeDaoMock).should().insertSafeInfo(capture(safeInfoCaptor))
        then(safeDaoMock).shouldHaveNoMoreInteractions()
        assertEquals(TEST_SAFE, safeInfoCaptor.value.safeAddress)
        assertEquals(owner, safeInfoCaptor.value.ownerAddress)
        assertEquals(cryptoData.toString(), EncryptedByteArray.Converter().toStorage(safeInfoCaptor.value.ownerPrivateKey))
        then(pushRepositoryMock).should().syncAuthentication(true)
        then(safeDaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun createOwner() {
        given(bip39.generateMnemonic(anyInt(), anyInt())).willReturn("some mnemonic")
        val seed = "08fc1d796483593d2ab1495511c560cbc2574d76ebce9efbe513b1eb006a8bbc71921b3c29c6229f7fc1d688c5012e199d30869efc57a7b538b7f31d0c1f7fe7"
        given(bip39.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(seed.hexToByteArray())

        val testObserver = TestObserver<Pair<Solidity.Address, ByteArray>>()
        repository.createOwner().subscribe(testObserver)
        val privateKey = "0xc956d2ca057d1c7ad15abff311bfc2f9cf6d396618c38a57332498ee78cf03dd".hexToByteArray()
        testObserver.assertSubscribed().assertNoErrors().assertComplete().assertValueCount(1)
        val generatedPair = testObserver.values().first()
        assertEquals("0xB7262c07974aA58fE8c705fD1f72C31E8A248f51".asEthereumAddress()!!, generatedPair.first)
        assertArrayEquals(privateKey, generatedPair.second)

        then(bip39).should().generateMnemonic(Bip39.MIN_ENTROPY_BITS, R.id.english)
        then(bip39).should().mnemonicToSeed("some mnemonic")
        then(bip39).shouldHaveNoMoreInteractions()
    }

    fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

    companion object {
        private val TEST_SAFE = "0xdeadfeedbeaf".asEthereumAddress()!!
    }
}
