package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Predicate
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
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.db.daos.GnosisSafeDao
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreation
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreationParams
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeDeployment
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toBytes
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
    private lateinit var relayServiceApiMock: RelayServiceApi

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var pushRepositoryMock: PushServiceRepository
    
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
            relayServiceApiMock,
            tokenRepositoryMock
        )
    }

    private fun calculateSafeAddress(setupData: String, saltNonce: Long): Solidity.Address {
        val setupDataHash = Sha3Utils.keccak(setupData.hexToByteArray())
        val salt = Sha3Utils.keccak(setupDataHash + Solidity.UInt256(saltNonce.toBigInteger()).encode().hexToByteArray())


        val deploymentCode = PROXY_CODE + MASTER_COPY_ADDRESS.encode()
        val codeHash = Sha3Utils.keccak(deploymentCode.hexToByteArray())
        val create2Hash = Sha3Utils.keccak(byteArrayOf(0xff.toByte()) + PROXY_FACTORY_ADDRESS.value.toBytes(20) + salt + codeHash)
        return Solidity.Address(BigInteger(1, create2Hash.copyOfRange(12, 32)))
    }

    private fun generateSafeCreationData(
        owners: List<Solidity.Address>,
        payment: BigInteger = BigInteger.ZERO,
        paymentToken: ERC20Token = ETHER_TOKEN,
        funder: Solidity.Address = TX_ORIGIN_ADDRESS,
        fallbackHandler: Solidity.Address = DEFAULT_FALLBACK_HANDLER
    ): String = GnosisSafe.Setup.encode(
        _owners = SolidityBase.Vector(owners),
        _threshold = Solidity.UInt256(if (owners.size == 3) BigInteger.ONE else 2.toBigInteger()),
        to = Solidity.Address(BigInteger.ZERO),
        data = Solidity.Bytes(byteArrayOf()),
        paymentToken = paymentToken.address,
        payment = Solidity.UInt256(payment),
        paymentReceiver = funder,
        fallbackHandler = fallbackHandler
    ) + "0000000000000000000000000000000000000000000000000000000000000000"

    @Test
    fun triggerSafeDeployment() {
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        var saltNonce: Long? = null
        var safeAddress: Solidity.Address? = null
        var response: RelaySafeCreation? = null

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any())).willReturn(Single.just(TEST_TOKEN))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            saltNonce = request.saltNonce
            val setupData = generateSafeCreationData(
                listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                BigInteger.TEN,
                TEST_TOKEN
            )
            safeAddress = calculateSafeAddress(setupData, saltNonce!!)
            response = RelaySafeCreation(
                setupData = setupData,
                safe = safeAddress!!,
                masterCopy = MASTER_COPY_ADDRESS,
                proxyFactory = PROXY_FACTORY_ADDRESS,
                payment = BigInteger.TEN,
                paymentToken = TEST_TOKEN.address,
                paymentReceiver = TX_ORIGIN_ADDRESS
            )
            Single.just(response!!)
        }

        val testObserver = TestObserver.create<SafeDeployment>()
        val owners = listOf(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)
        repository.triggerSafeDeployment(owners, 2).subscribe(testObserver)

        testObserver.assertResult(SafeDeployment(safeAddress!!, TEST_TOKEN.address, BigInteger.TEN))

        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

        then(relayServiceApiMock).should()
            .safeCreation(
                RelaySafeCreationParams(
                    listOf(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                    2, saltNonce!!, TEST_TOKEN.address
                )
            )
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
    }


    @Test
    fun triggerSafeDeploymentAddressesNotMatching() {
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val safeAddress = "ffff".asEthereumAddress()!!
        var saltNonce: Long? = null
        var response: RelaySafeCreation?

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any())).willReturn(Single.just(ETHER_TOKEN))
    
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            saltNonce = request.saltNonce
            val setupData = generateSafeCreationData(
                listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                BigInteger.ZERO, ETHER_TOKEN, FUNDER_ADDRESS
            )
            response = RelaySafeCreation(
                setupData = setupData,
                safe = safeAddress,
                masterCopy = MASTER_COPY_ADDRESS,
                proxyFactory = PROXY_FACTORY_ADDRESS,
                payment = BigInteger.ZERO,
                paymentToken = ETHER_TOKEN.address,
                paymentReceiver = FUNDER_ADDRESS
            )
            Single.just(response!!)
        }

        val testObserver = TestObserver.create<SafeDeployment>()
        val owners = listOf(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)
        repository.triggerSafeDeployment(owners, 2).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

        then(relayServiceApiMock).should()
            .safeCreation(
                RelaySafeCreationParams(
                    listOf(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                    2, saltNonce!!, ETHER_TOKEN.address
                )
            )
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()

        testObserver.assertFailure(Predicate { error ->
            error is IllegalStateException && error.message == "Unexpected safe address returned"
        })
    }

    private fun testSafeCreationChecks(
        ownerAddress: Solidity.Address,
        mnemonicAddress0: Solidity.Address,
        mnemonicAddress1: Solidity.Address,
        chromeExtensionAddress: Solidity.Address?,
        responseAnswer: (RelaySafeCreationParams) -> Single<RelaySafeCreation>,
        failurePredicate: Predicate<Throwable>,
        paymentToken: ERC20Token = ETHER_TOKEN
    ) {
        var saltNonce: Long? = null

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any())).willReturn(Single.just(paymentToken))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            saltNonce = request.saltNonce
            responseAnswer(request)
        }

        val testObserver = TestObserver.create<SafeDeployment>()
        val owners = listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)
        repository.triggerSafeDeployment(owners, 2).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(
                listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                chromeExtensionAddress?.run { 2 } ?: 1, saltNonce!!, paymentToken.address)
            )
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()

        testObserver.assertFailure(failurePredicate)
    }

    @Test
    fun triggerSafeDeploymentDifferentPaymentToken() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = generateSafeCreationData(
                            listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                            BigInteger.ZERO, PAYMENT_TOKEN
                        ),
                        safe = safeAddress,
                        masterCopy = MASTER_COPY_ADDRESS,
                        proxyFactory = PROXY_FACTORY_ADDRESS,
                        payment = BigInteger.ZERO,
                        paymentToken = PAYMENT_TOKEN.address,
                        paymentReceiver = TX_ORIGIN_ADDRESS
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected payment token returned"
            }
        )
    }

    @Test
    fun triggerSafeDeploymentDifferentMasterCopy() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = generateSafeCreationData(
                            listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                            BigInteger.ZERO, ETHER_TOKEN
                        ),
                        safe = safeAddress,
                        masterCopy = PAYMENT_TOKEN.address,
                        proxyFactory = PROXY_FACTORY_ADDRESS,
                        payment = BigInteger.ZERO,
                        paymentToken = ETHER_TOKEN.address,
                        paymentReceiver = TX_ORIGIN_ADDRESS
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected master copy returned"
            }
        )
    }

    @Test
    fun triggerSafeDeploymentDifferentProxyFactory() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = generateSafeCreationData(
                            listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                            BigInteger.ZERO, ETHER_TOKEN
                        ),
                        safe = safeAddress,
                        masterCopy = MASTER_COPY_ADDRESS,
                        proxyFactory = PAYMENT_TOKEN.address,
                        payment = BigInteger.ZERO,
                        paymentToken = ETHER_TOKEN.address,
                        paymentReceiver = TX_ORIGIN_ADDRESS
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected proxy factory returned"
            }
        )
    }

    @Test
    fun triggerSafeDeploymentUnknownReceiver() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = generateSafeCreationData(
                            listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                            BigInteger.ZERO, ETHER_TOKEN, PAYMENT_TOKEN.address
                        ),
                        safe = safeAddress,
                        masterCopy = MASTER_COPY_ADDRESS,
                        proxyFactory = PROXY_FACTORY_ADDRESS,
                        payment = BigInteger.ZERO,
                        paymentToken = ETHER_TOKEN.address,
                        paymentReceiver = PAYMENT_TOKEN.address
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected payment receiver returned"
            }
        )
    }

    @Test
    fun triggerSafeDeploymentInvalidSetupData() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = "0000000000000000000000000000000000000000000000000000000000000000",
                        safe = safeAddress,
                        masterCopy = MASTER_COPY_ADDRESS,
                        proxyFactory = PROXY_FACTORY_ADDRESS,
                        payment = BigInteger.ZERO,
                        paymentToken = ETHER_TOKEN.address,
                        paymentReceiver = TX_ORIGIN_ADDRESS
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected setup data returned"
            }
        )
    }

    @Test
    fun triggerSafeDeploymentDifferentOwnersInData() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = generateSafeCreationData(
                            listOfNotNull(PAYMENT_TOKEN.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                            BigInteger.ZERO, ETHER_TOKEN
                        ),
                        safe = safeAddress,
                        masterCopy = MASTER_COPY_ADDRESS,
                        proxyFactory = PROXY_FACTORY_ADDRESS,
                        payment = BigInteger.ZERO,
                        paymentToken = ETHER_TOKEN.address,
                        paymentReceiver = TX_ORIGIN_ADDRESS
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected setup data returned"
            }
        )
    }

    @Test
    fun triggerSafeDeploymentDifferentPaymentReceiverInData() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = generateSafeCreationData(
                            listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                            BigInteger.ZERO, ETHER_TOKEN, PAYMENT_TOKEN.address
                        ),
                        safe = safeAddress,
                        masterCopy = MASTER_COPY_ADDRESS,
                        proxyFactory = PROXY_FACTORY_ADDRESS,
                        payment = BigInteger.ZERO,
                        paymentToken = ETHER_TOKEN.address,
                        paymentReceiver = TX_ORIGIN_ADDRESS
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected setup data returned"
            }
        )
    }

    @Test
    fun triggerSafeDeploymentDifferentPaymentTokenInData() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = generateSafeCreationData(
                            listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                            BigInteger.ZERO, PAYMENT_TOKEN, TX_ORIGIN_ADDRESS
                        ),
                        safe = safeAddress,
                        masterCopy = MASTER_COPY_ADDRESS,
                        proxyFactory = PROXY_FACTORY_ADDRESS,
                        payment = BigInteger.ZERO,
                        paymentToken = ETHER_TOKEN.address,
                        paymentReceiver = TX_ORIGIN_ADDRESS
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected setup data returned"
            }
        )
    }

    @Test
    fun triggerSafeDeploymentDifferentPaymentInData() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val ownerAddress = Solidity.Address(10.toBigInteger())
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            ownerAddress, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        setupData = generateSafeCreationData(
                            listOfNotNull(ownerAddress, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                            BigInteger.TEN, ETHER_TOKEN, TX_ORIGIN_ADDRESS
                        ),
                        safe = safeAddress,
                        masterCopy = MASTER_COPY_ADDRESS,
                        proxyFactory = PROXY_FACTORY_ADDRESS,
                        payment = BigInteger.ZERO,
                        paymentToken = ETHER_TOKEN.address,
                        paymentReceiver = TX_ORIGIN_ADDRESS
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected setup data returned"
            }
        )
    }

    @Test
    fun checkSafeInvalidMasterCopyThresholdTooLow() {
        val masterCopy = "0xdeadbeef".asEthereumAddress()!!
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            val ethGetStorageAt = requests[0] as EthGetStorageAt
            assertEquals(TEST_SAFE, ethGetStorageAt.from)
            assertEquals(BigInteger.ZERO, ethGetStorageAt.location)
            assertEquals(Block.PENDING, ethGetStorageAt.block)
            ethGetStorageAt.response = EthRequest.Response.Success(masterCopy.encode())

            (requests[1] as EthCall).response = EthRequest.Response.Success(Solidity.UInt256(BigInteger.ONE).encode())
            Observable.just(bulk)
        }
        val observer = TestObserver<Pair<Solidity.Address?, Boolean>>()
        repository.checkSafe(TEST_SAFE).subscribe(observer)
        observer.assertResult(masterCopy to false)
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
        val observer = TestObserver<Pair<Solidity.Address?, Boolean>>()
        repository.checkSafe(TEST_SAFE).subscribe(observer)
        observer.assertResult(null to false)
        then(ethereumRepositoryMock).should().request(MockUtils.any<BulkRequest>())
    }

    @Test
    fun checkSafeThresholdFailure() {
        val masterCopy = "0xdeadbeef".asEthereumAddress()!!
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            val ethGetStorageAt = requests[0] as EthGetStorageAt
            assertEquals(TEST_SAFE, ethGetStorageAt.from)
            assertEquals(BigInteger.ZERO, ethGetStorageAt.location)
            assertEquals(Block.PENDING, ethGetStorageAt.block)
            ethGetStorageAt.response = EthRequest.Response.Success(masterCopy.encode())
            (requests[1] as EthCall).response = EthRequest.Response.Failure("EVM error")
            Observable.just(bulk)
        }
        val observer = TestObserver<Pair<Solidity.Address?, Boolean>>()
        repository.checkSafe(TEST_SAFE).subscribe(observer)
        observer.assertResult(masterCopy to false)
        then(ethereumRepositoryMock).should().request(MockUtils.any<BulkRequest>())
    }

    private fun testMasterCopy(masterCopy: String, threshold: Int) {
        val masterCopyAddress = masterCopy.asEthereumAddress()!!
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            val ethGetStorageAt = requests[0] as EthGetStorageAt
            assertEquals(TEST_SAFE, ethGetStorageAt.from)
            assertEquals(BigInteger.ZERO, ethGetStorageAt.location)
            assertEquals(Block.PENDING, ethGetStorageAt.block)
            ethGetStorageAt.response = EthRequest.Response.Success(masterCopyAddress.encode())
                (requests[1] as EthCall).response = EthRequest.Response.Success(Solidity.UInt256(BigInteger.valueOf(threshold.toLong())).encode())
            Observable.just(bulk)
        }
        val observer = TestObserver<Pair<Solidity.Address?, Boolean>>()
        repository.checkSafe(TEST_SAFE).subscribe(observer)
        observer.assertResult(masterCopyAddress to true)
        then(ethereumRepositoryMock).should().request(MockUtils.any<BulkRequest>())
        Mockito.reset(ethereumRepositoryMock)
    }

    @Test
    fun checkSafe() {
        val masterCopyAddresses = listOf(
            BuildConfig.SAFE_MASTER_COPY_0_0_2,
            BuildConfig.SAFE_MASTER_COPY_0_1_0,
            BuildConfig.SAFE_MASTER_COPY_1_0_0
        )
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

    companion object {
        private val TEST_SAFE = "0xdeadfeedbeaf".asEthereumAddress()!!
        private val TEST_TOKEN = ERC20Token(Solidity.Address(BigInteger.ONE), "Hello Token", "HT", 10)
        private val ETHER_TOKEN = ERC20Token.ETHER_TOKEN
        private val PAYMENT_TOKEN = ERC20Token("0xdeadbeef".asEthereumAddress()!!, "Payment Token", "PT", 18)
        private val FUNDER_ADDRESS = BuildConfig.SAFE_CREATION_FUNDER.asEthereumAddress()!!
        private val TX_ORIGIN_ADDRESS = "0x0".asEthereumAddress()!!
        private val MASTER_COPY_ADDRESS = "0x34CfAC646f301356fAa8B21e94227e3583Fe3F5F".asEthereumAddress()!!
        private val PROXY_FACTORY_ADDRESS = "0x76E2cFc1F5Fa8F6a5b3fC4c8F4788F0116861F9B".asEthereumAddress()!!
        private val DEFAULT_FALLBACK_HANDLER = "0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44".asEthereumAddress()!!
        private const val PROXY_CODE =
            "0x608060405234801561001057600080fd5b506040516101e73803806101e78339818101604052602081101561003357600080fd5b8101908080519060200190929190505050600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1614156100ca576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001806101c36024913960400191505060405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505060aa806101196000396000f3fe608060405273ffffffffffffffffffffffffffffffffffffffff600054167fa619486e0000000000000000000000000000000000000000000000000000000060003514156050578060005260206000f35b3660008037600080366000845af43d6000803e60008114156070573d6000fd5b3d6000f3fea265627a7a72315820d8a00dc4fe6bf675a9d7416fc2d00bb3433362aa8186b750f76c4027269667ff64736f6c634300050e0032496e76616c6964206d617374657220636f707920616464726573732070726f7669646564"
    }
}
