package pm.gnosis.heimdall.data.repositories.impls

import android.app.Application
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.data.remote.MessageQueueRepository
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.models.RequestSignatureData
import pm.gnosis.heimdall.data.remote.models.SendSignatureData
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.removeHexPrefix
import pm.gnosis.utils.toHexString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class DefaultSignaturePushRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private val preferences = TestPreferences()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var accountRepoMock: AccountsRepository

    @Mock
    private lateinit var messagingQueueRepositoryMock: MessageQueueRepository

    @Mock
    private lateinit var pushServiceApiMock: PushServiceApi

    @Mock
    private lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var transactionRepositoryMock: GnosisSafeTransactionRepository

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: DefaultSignaturePushRepository

    @Before
    fun setUp() {
        given(application.getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).willReturn(preferences)
        preferencesManager = PreferencesManager(application)
        repository = DefaultSignaturePushRepository(
            accountRepoMock,
            messagingQueueRepositoryMock,
            preferencesManager,
            pushServiceApiMock,
            safeRepositoryMock,
            transactionRepositoryMock
        )
    }

    private fun cleanAddress(address: BigInteger) =
        address.asEthereumAddressString().toLowerCase().removeHexPrefix()

    @Test
    fun initEmptySafes() {
        given(safeRepositoryMock.observeDeployedSafes()).willReturn(Flowable.empty())
        preferences.putStringSet(PREFS_OBSERVED_SAFES, null)
        repository.init()

        then(messagingQueueRepositoryMock).shouldHaveNoMoreInteractions()

        then(safeRepositoryMock).should().observeDeployedSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initWithSafes() {
        val safeProcessor = PublishProcessor.create<List<Safe>>()
        given(safeRepositoryMock.observeDeployedSafes()).willReturn(safeProcessor)

        val address1 = cleanAddress(TEST_SAFE_1)
        val address2 = cleanAddress(TEST_SAFE_2)
        preferences.putStringSet(PREFS_OBSERVED_SAFES, mutableSetOf(address1, address2))

        repository.init()

        then(safeRepositoryMock).should().observeDeployedSafes()

        then(messagingQueueRepositoryMock).should().unsubscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + address1)
        then(messagingQueueRepositoryMock).should().unsubscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + address2)

        then(messagingQueueRepositoryMock).shouldHaveNoMoreInteractions()

        assertNull(preferences.getStringSet(PREFS_OBSERVED_SAFES, null))

        safeProcessor.onNext(listOf(Safe(TEST_SAFE_1, "Safe 1"), Safe(TEST_SAFE_2, "Safe 2")))

        then(messagingQueueRepositoryMock).should().subscribe(REQUEST_SIGNATURE_TOPIC_PREFIX + address1)
        then(messagingQueueRepositoryMock).should().subscribe(REQUEST_SIGNATURE_TOPIC_PREFIX + address2)

        then(messagingQueueRepositoryMock).shouldHaveNoMoreInteractions()

        safeProcessor.onNext(listOf(Safe(TEST_SAFE_2, "Safe 2")))

        then(messagingQueueRepositoryMock).should().unsubscribe(REQUEST_SIGNATURE_TOPIC_PREFIX + address1)
        then(messagingQueueRepositoryMock).should().unsubscribe(REQUEST_SIGNATURE_TOPIC_PREFIX + address2)

        then(messagingQueueRepositoryMock).should(times(2)).subscribe(REQUEST_SIGNATURE_TOPIC_PREFIX + address2)

        then(messagingQueueRepositoryMock).shouldHaveNoMoreInteractions()

        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun request() {
        given(accountRepoMock.sign(MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE))
        given(pushServiceApiMock.requestSignatures(anyString(), anyString(), MockUtils.any()))
            .willReturn(Completable.complete())

        val testObserver = TestObserver<Unit>()
        repository.request(TEST_SAFE_1, "some_request_url").subscribe(testObserver)

        testObserver.assertResult()

        then(accountRepoMock).should().sign(Sha3Utils.keccak(cleanAddress(TEST_SAFE_1).hexStringToByteArray()))
        then(pushServiceApiMock).should()
            .requestSignatures(TEST_SIGNATURE.toString(), cleanAddress(TEST_SAFE_1), RequestSignatureData("some_request_url"))

        then(accountRepoMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun requestRequestFailure() {
        val error = IllegalStateException()
        given(accountRepoMock.sign(MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE))
        given(pushServiceApiMock.requestSignatures(anyString(), anyString(), MockUtils.any()))
            .willReturn(Completable.error(error))

        val testObserver = TestObserver<Unit>()
        repository.request(TEST_SAFE_1, "some_request_url").subscribe(testObserver)

        testObserver.assertFailure(Predicate { it == error })

        then(accountRepoMock).should().sign(Sha3Utils.keccak(cleanAddress(TEST_SAFE_1).hexStringToByteArray()))
        then(pushServiceApiMock).should()
            .requestSignatures(TEST_SIGNATURE.toString(), cleanAddress(TEST_SAFE_1), RequestSignatureData("some_request_url"))

        then(accountRepoMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun requestSignFailure() {
        val error = IllegalStateException()
        given(accountRepoMock.sign(MockUtils.any())).willReturn(Single.error(error))

        val testObserver = TestObserver<Unit>()
        repository.request(TEST_SAFE_1, "some_request_url").subscribe(testObserver)

        testObserver.assertFailure(Predicate { it == error })

        then(accountRepoMock).should().sign(Sha3Utils.keccak(cleanAddress(TEST_SAFE_1).hexStringToByteArray()))

        then(accountRepoMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun send() {
        val testHash = "ThisShouldBeAHash".toByteArray()
        given(transactionRepositoryMock.calculateHash(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(testHash))

        given(pushServiceApiMock.sendSignature(anyString(), MockUtils.any()))
            .willReturn(Completable.complete())

        val testObserver = TestObserver<Unit>()
        repository.send(TEST_SAFE_1, TEST_TRANSACTION, TEST_SIGNATURE).subscribe(testObserver)

        testObserver.assertResult()

        then(transactionRepositoryMock).should().calculateHash(TEST_SAFE_1, TEST_TRANSACTION)
        then(pushServiceApiMock).should()
            .sendSignature(cleanAddress(TEST_SAFE_1), SendSignatureData(GnoSafeUrlParser.signResponse(TEST_SIGNATURE), testHash.toHexString()))

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun sendRequestFailure() {
        val error = IllegalStateException()
        val testHash = "ThisShouldBeAHash".toByteArray()
        given(transactionRepositoryMock.calculateHash(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(testHash))

        given(pushServiceApiMock.sendSignature(anyString(), MockUtils.any()))
            .willReturn(Completable.error(error))

        val testObserver = TestObserver<Unit>()
        repository.send(TEST_SAFE_1, TEST_TRANSACTION, TEST_SIGNATURE).subscribe(testObserver)

        testObserver.assertFailure(Predicate { it == error })

        then(transactionRepositoryMock).should().calculateHash(TEST_SAFE_1, TEST_TRANSACTION)
        then(pushServiceApiMock).should()
            .sendSignature(cleanAddress(TEST_SAFE_1), SendSignatureData(GnoSafeUrlParser.signResponse(TEST_SIGNATURE), testHash.toHexString()))

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun sendHashFailure() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.calculateHash(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(error))

        val testObserver = TestObserver<Unit>()
        repository.send(TEST_SAFE_1, TEST_TRANSACTION, TEST_SIGNATURE).subscribe(testObserver)

        testObserver.assertFailure(Predicate { it == error })

        then(transactionRepositoryMock).should().calculateHash(TEST_SAFE_1, TEST_TRANSACTION)

        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeAndReceive() {
        val safe1Observer = TestObserver<Signature>()
        val safe2aObserver = TestObserver<Signature>()
        val safe2bObserver = TestObserver<Signature>()
        repository.observe(TEST_SAFE_1).subscribe(safe1Observer)
        repository.observe(TEST_SAFE_2).subscribe(safe2aObserver)
        repository.observe(TEST_SAFE_2).subscribe(safe2bObserver)

        then(messagingQueueRepositoryMock).should().subscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + cleanAddress(TEST_SAFE_1))
        then(messagingQueueRepositoryMock).should().subscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + cleanAddress(TEST_SAFE_2))
        then(messagingQueueRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(preferences.getStringSet(PREFS_OBSERVED_SAFES, null), mutableSetOf(cleanAddress(TEST_SAFE_1), cleanAddress(TEST_SAFE_2)))

        // Invalid topic or non tracked should not propagate signature
        repository.receivedSignature(null, TEST_SIGNATURE)
        repository.receivedSignature("", TEST_SIGNATURE)
        repository.receivedSignature("some_invalid_topic", TEST_SIGNATURE)
        repository.receivedSignature(FCM_TOPICS_PREFIX + RESPOND_SIGNATURE_TOPIC_PREFIX + cleanAddress(BigInteger.TEN), TEST_SIGNATURE)
        safe1Observer.assertEmpty()
        safe2aObserver.assertEmpty()
        safe2bObserver.assertEmpty()

        repository.receivedSignature(FCM_TOPICS_PREFIX + RESPOND_SIGNATURE_TOPIC_PREFIX + cleanAddress(TEST_SAFE_1), TEST_SIGNATURE)
        safe1Observer.assertValuesOnly(TEST_SIGNATURE)
        safe2aObserver.assertEmpty()
        safe2bObserver.assertEmpty()

        repository.receivedSignature(FCM_TOPICS_PREFIX + RESPOND_SIGNATURE_TOPIC_PREFIX + cleanAddress(TEST_SAFE_2), TEST_SIGNATURE_2)
        safe1Observer.assertValuesOnly(TEST_SIGNATURE) // No new values
        safe2aObserver.assertValuesOnly(TEST_SIGNATURE_2)
        safe2bObserver.assertValuesOnly(TEST_SIGNATURE_2)

        safe2aObserver.dispose()

        then(messagingQueueRepositoryMock).shouldHaveNoMoreInteractions()

        safe2bObserver.dispose()
        assertEquals(preferences.getStringSet(PREFS_OBSERVED_SAFES, null), mutableSetOf(cleanAddress(TEST_SAFE_1)))
        then(messagingQueueRepositoryMock).should().unsubscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + cleanAddress(TEST_SAFE_2))
        then(messagingQueueRepositoryMock).shouldHaveNoMoreInteractions()

        safe1Observer.dispose()
        assertEquals(preferences.getStringSet(PREFS_OBSERVED_SAFES, null), emptySet<String>())
        then(messagingQueueRepositoryMock).should().unsubscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + cleanAddress(TEST_SAFE_1))
        then(messagingQueueRepositoryMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE_1 = BigInteger.valueOf(65489720)
        private val TEST_SAFE_2 = BigInteger.valueOf(2589631)
        private val TEST_SIGNATURE = Signature(BigInteger.valueOf(987), BigInteger.valueOf(678), 27)
        private val TEST_SIGNATURE_2 = Signature(BigInteger.valueOf(6789), BigInteger.valueOf(1234), 28)
        private val TEST_TRANSACTION = Transaction(BigInteger.ZERO, nonce = BigInteger.TEN)
        private const val PREFS_OBSERVED_SAFES = "default_signature_push_repo.string_set.observed_safes"
        private const val REQUEST_SIGNATURE_TOPIC_PREFIX = "request_signature."
        private const val RESPOND_SIGNATURE_TOPIC_PREFIX = "respond_signature."
        private const val FCM_TOPICS_PREFIX = "/topics/"
    }
}
