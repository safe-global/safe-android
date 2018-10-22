package pm.gnosis.heimdall.data.repositories.impls

import android.app.Activity
import android.content.Context
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.*
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.models.push.*
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import java.util.*
import java.util.concurrent.Executor

@RunWith(MockitoJUnitRunner::class)
class DefaultPushServiceRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var firebaseInstanceIdMock: FirebaseInstanceId

    @Mock
    private lateinit var localNotificationManagerMock: LocalNotificationManager

    @Mock
    private lateinit var moshiMock: Moshi

    @Mock
    private lateinit var preferencesManagerMock: PreferencesManager

    @Mock
    private lateinit var pushServiceApiMock: PushServiceApi

    @Mock
    private lateinit var safeCreationJsonAdapter: JsonAdapter<ServiceMessage.SafeCreation>

    @Mock
    private lateinit var sendTransactionHashJsonAdapter: JsonAdapter<ServiceMessage.SendTransactionHash>

    @Mock
    private lateinit var rejectTransactionJsonAdapter: JsonAdapter<ServiceMessage.RejectTransaction>

    @Mock
    private lateinit var requestConfirmationJsonAdapter: JsonAdapter<ServiceMessage.RequestConfirmation>

    @Captor
    private lateinit var stringsArgumentCaptor: ArgumentCaptor<String>

    private val testPreferences = TestPreferences()

    private lateinit var pushServiceRepository: DefaultPushServiceRepository

    @Before
    fun setUp() {
        pushServiceRepository = DefaultPushServiceRepository(
            contextMock,
            accountsRepositoryMock,
            firebaseInstanceIdMock,
            localNotificationManagerMock,
            moshiMock,
            preferencesManagerMock,
            pushServiceApiMock
        )
        given(preferencesManagerMock.prefs).willReturn(testPreferences)
    }

    @Test
    fun syncAuthenticationNewToken() {
        val testFirebaseInstanceIdTask = TestFirebaseInstanceIdTask()
        given(firebaseInstanceIdMock.instanceId).willReturn(testFirebaseInstanceIdTask)
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(TEST_ACCOUNT))
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE))
        given(pushServiceApiMock.auth(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.syncAuthentication()
        testFirebaseInstanceIdTask.setSuccess(TestInstanceIdResult())

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("GNO${testFirebaseInstanceIdTask.result.token}".toByteArray()))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(
            "${TEST_ACCOUNT.address.asEthereumAddressString()}${testFirebaseInstanceIdTask.result.token}",
            testPreferences.getString(LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY, "")
        )
        then(firebaseInstanceIdMock).should().instanceId
        then(firebaseInstanceIdMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).should().auth(
            PushServiceAuth(
                pushToken = testFirebaseInstanceIdTask.result.token,
                signature = ServiceSignature.fromSignature(TEST_SIGNATURE)
            )
        )
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
    }

    @Test
    fun syncAuthenticationSameToken() {
        val testFirebaseInstanceIdTask = TestFirebaseInstanceIdTask()
        given(firebaseInstanceIdMock.instanceId).willReturn(testFirebaseInstanceIdTask)
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(TEST_ACCOUNT))

        // Set last synced token (same as new one)
        val testInstanceIdResult = TestInstanceIdResult()
        testPreferences.putString(
            LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY,
            "${TEST_ACCOUNT.address.asEthereumAddressString()}${testInstanceIdResult.token}"
        )
        pushServiceRepository.syncAuthentication()
        testFirebaseInstanceIdTask.setSuccess(testInstanceIdResult)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(firebaseInstanceIdMock).should().instanceId
        then(firebaseInstanceIdMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
    }

    @Test
    fun syncAuthenticationForced() {
        val testFirebaseInstanceIdTask = TestFirebaseInstanceIdTask()
        given(firebaseInstanceIdMock.instanceId).willReturn(testFirebaseInstanceIdTask)
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(TEST_ACCOUNT))
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE))
        given(pushServiceApiMock.auth(MockUtils.any())).willReturn(Completable.complete())

        // Set last synced token (same as new one)
        val testInstanceIdResult = TestInstanceIdResult()
        testPreferences.putString(
            LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY,
            "${TEST_ACCOUNT.address.asEthereumAddressString()}${testInstanceIdResult.token}"
        )
        pushServiceRepository.syncAuthentication(forced = true)
        testFirebaseInstanceIdTask.setSuccess(testInstanceIdResult)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("GNO${testFirebaseInstanceIdTask.result.token}".toByteArray()))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(
            "${TEST_ACCOUNT.address.asEthereumAddressString()}${testFirebaseInstanceIdTask.result.token}",
            testPreferences.getString(LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY, "")
        )
        then(firebaseInstanceIdMock).should().instanceId
        then(firebaseInstanceIdMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).should().auth(
            PushServiceAuth(
                pushToken = testFirebaseInstanceIdTask.result.token,
                signature = ServiceSignature.fromSignature(TEST_SIGNATURE)
            )
        )
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
    }

    @Test
    fun syncAuthenticationInstanceIdError() {
        val testFirebaseInstanceIdTask = TestFirebaseInstanceIdTask()
        val exception = Exception()
        given(firebaseInstanceIdMock.instanceId).willReturn(testFirebaseInstanceIdTask)
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(TEST_ACCOUNT))

        // Set last synced token
        val testInstanceIdResult = TestInstanceIdResult()
        testPreferences.putString(
            LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY,
            "${TEST_ACCOUNT.address.asEthereumAddressString()}${testInstanceIdResult.token}"
        )
        pushServiceRepository.syncAuthentication()
        testFirebaseInstanceIdTask.setFailure(exception)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        // If the FirebaseInstanceId fails then the stored token should not change
        assertEquals(
            "${TEST_ACCOUNT.address.asEthereumAddressString()}${testInstanceIdResult.token}",
            testPreferences.getString(LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY, "")
        )
        then(firebaseInstanceIdMock).should().instanceId
        then(firebaseInstanceIdMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
    }

    @Test
    fun pair() {
        val testObserver = TestObserver<Solidity.Address>()
        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        given(accountsRepositoryMock.recover(MockUtils.any(), MockUtils.any())).willReturn(Single.just(TEST_EXTENSION_ADDRESS))
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.pair(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.pair(pushServiceTemporaryAuthorization).subscribe(testObserver)

        then(accountsRepositoryMock).should().recover(
            data = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(accountsRepositoryMock).should()
            .sign(Sha3Utils.keccak("$SIGNATURE_PREFIX${TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()}".toByteArray()))
        then(pushServiceApiMock).should().pair(
            PushServicePairing(
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE),
                temporaryAuthorization = pushServiceTemporaryAuthorization
            )
        )
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(preferencesManagerMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertResult(TEST_EXTENSION_ADDRESS)
    }

    @Test
    fun pairPushServiceApiError() {
        val testObserver = TestObserver<Solidity.Address>()
        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        val exception = IllegalStateException()
        given(accountsRepositoryMock.recover(MockUtils.any(), MockUtils.any())).willReturn(Single.just(TEST_EXTENSION_ADDRESS))
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.pair(MockUtils.any())).willReturn(Completable.error(exception))

        pushServiceRepository.pair(pushServiceTemporaryAuthorization).subscribe(testObserver)

        then(accountsRepositoryMock).should().recover(
            data = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(accountsRepositoryMock).should()
            .sign(Sha3Utils.keccak("$SIGNATURE_PREFIX${TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()}".toByteArray()))
        then(pushServiceApiMock).should().pair(
            PushServicePairing(
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE),
                temporaryAuthorization = pushServiceTemporaryAuthorization
            )
        )
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(preferencesManagerMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun pairSignError() {
        val testObserver = TestObserver<Solidity.Address>()
        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        val exception = IllegalStateException()
        given(accountsRepositoryMock.recover(MockUtils.any(), MockUtils.any())).willReturn(Single.just(TEST_EXTENSION_ADDRESS))
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.pair(pushServiceTemporaryAuthorization).subscribe(testObserver)

        then(accountsRepositoryMock).should().recover(
            data = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(accountsRepositoryMock).should()
            .sign(Sha3Utils.keccak("$SIGNATURE_PREFIX${TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()}".toByteArray()))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(preferencesManagerMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun pairRecoverError() {
        val testObserver = TestObserver<Solidity.Address>()
        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        val exception = IllegalStateException()
        given(accountsRepositoryMock.recover(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.pair(pushServiceTemporaryAuthorization).subscribe(testObserver)

        then(accountsRepositoryMock).should().recover(
            data = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(preferencesManagerMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateSafeCreation() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.SafeCreation(safe = TEST_SAFE_ADDRESS.asEthereumAddressString())
        given(moshiMock.adapter<ServiceMessage.SafeCreation>(MockUtils.any())).willReturn(safeCreationJsonAdapter)
        given(safeCreationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.propagateSafeCreation(
            safeAddress = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SafeCreation::class.java)
        then(safeCreationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressString()),
                message = json,
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE)
            )
        )
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(safeCreationJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertNoErrors().assertComplete()
    }

    @Test
    fun propagateSafeCreationErrorNotifying() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.SafeCreation(safe = TEST_SAFE_ADDRESS.asEthereumAddressString())
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.SafeCreation>(MockUtils.any())).willReturn(safeCreationJsonAdapter)
        given(safeCreationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.error(exception))

        pushServiceRepository.propagateSafeCreation(
            safeAddress = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SafeCreation::class.java)
        then(safeCreationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressString()),
                message = json,
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE)
            )
        )
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(safeCreationJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateSafeCreationErrorSigning() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.SafeCreation(safe = TEST_SAFE_ADDRESS.asEthereumAddressString())
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.SafeCreation>(MockUtils.any())).willReturn(safeCreationJsonAdapter)
        given(safeCreationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.propagateSafeCreation(
            safeAddress = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SafeCreation::class.java)
        then(safeCreationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(safeCreationJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateSafeCreationErrorParsing() {
        val testObserver = TestObserver.create<Unit>()
        val message = ServiceMessage.SafeCreation(safe = TEST_SAFE_ADDRESS.asEthereumAddressString())
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.SafeCreation>(MockUtils.any())).willReturn(safeCreationJsonAdapter)
        given(safeCreationJsonAdapter.toJson(MockUtils.any())).willThrow(exception)

        pushServiceRepository.propagateSafeCreation(
            safeAddress = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SafeCreation::class.java)
        then(safeCreationJsonAdapter).should().toJson(message)
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(safeCreationJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateSubmittedTransaction() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.SendTransactionHash(
            hash = "hash",
            txHash = "txHash"
        )
        given(moshiMock.adapter<ServiceMessage.SendTransactionHash>(MockUtils.any())).willReturn(sendTransactionHashJsonAdapter)
        given(sendTransactionHashJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.propagateSubmittedTransaction(
            hash = message.hash,
            chainHash = message.txHash,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SendTransactionHash::class.java)
        then(sendTransactionHashJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressString()),
                message = json,
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE)
            )
        )
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(sendTransactionHashJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertNoErrors().assertComplete()
    }

    @Test
    fun propagateSubmittedTransactionErrorNotifying() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.SendTransactionHash(
            hash = "hash",
            txHash = "txHash"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.SendTransactionHash>(MockUtils.any())).willReturn(sendTransactionHashJsonAdapter)
        given(sendTransactionHashJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.error(exception))

        pushServiceRepository.propagateSubmittedTransaction(
            hash = message.hash,
            chainHash = message.txHash,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SendTransactionHash::class.java)
        then(sendTransactionHashJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressString()),
                message = json,
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE)
            )
        )
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(sendTransactionHashJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateSubmittedTransactionErrorSigning() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.SendTransactionHash(
            hash = "hash",
            txHash = "txHash"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.SendTransactionHash>(MockUtils.any())).willReturn(sendTransactionHashJsonAdapter)
        given(sendTransactionHashJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.propagateSubmittedTransaction(
            hash = message.hash,
            chainHash = message.txHash,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SendTransactionHash::class.java)
        then(sendTransactionHashJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(sendTransactionHashJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateSubmittedTransactionErrorParsing() {
        val testObserver = TestObserver.create<Unit>()
        val message = ServiceMessage.SendTransactionHash(
            hash = "hash",
            txHash = "txHash"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.SendTransactionHash>(MockUtils.any())).willReturn(sendTransactionHashJsonAdapter)
        given(sendTransactionHashJsonAdapter.toJson(MockUtils.any())).willThrow(exception)

        pushServiceRepository.propagateSubmittedTransaction(
            hash = message.hash,
            chainHash = message.txHash,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SendTransactionHash::class.java)
        then(sendTransactionHashJsonAdapter).should().toJson(message)
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(sendTransactionHashJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateTransactionRejected() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.RejectTransaction(
            hash = "hash",
            r = "10",
            s = "1",
            v = "27"
        )
        given(moshiMock.adapter<ServiceMessage.RejectTransaction>(MockUtils.any())).willReturn(rejectTransactionJsonAdapter)
        given(rejectTransactionJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.propagateTransactionRejected(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            signature = TEST_SIGNATURE
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RejectTransaction::class.java)
        then(rejectTransactionJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressString()),
                message = json,
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE)
            )
        )
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(rejectTransactionJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertNoErrors().assertComplete()
    }

    @Test
    fun propagateTransactionRejectedErrorNotifying() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.RejectTransaction(
            hash = "hash",
            r = "10",
            s = "1",
            v = "27"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.RejectTransaction>(MockUtils.any())).willReturn(rejectTransactionJsonAdapter)
        given(rejectTransactionJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.error(exception))

        pushServiceRepository.propagateTransactionRejected(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            signature = TEST_SIGNATURE
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RejectTransaction::class.java)
        then(rejectTransactionJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressString()),
                message = json,
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE)
            )
        )
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(rejectTransactionJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateTransactionRejectedErrorSigning() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.RejectTransaction(
            hash = "hash",
            r = "10",
            s = "1",
            v = "27"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.RejectTransaction>(MockUtils.any())).willReturn(rejectTransactionJsonAdapter)
        given(rejectTransactionJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.propagateTransactionRejected(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            signature = TEST_SIGNATURE
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RejectTransaction::class.java)
        then(rejectTransactionJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(rejectTransactionJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateTransactionRejectedErrorParsing() {
        val testObserver = TestObserver.create<Unit>()
        val message = ServiceMessage.RejectTransaction(
            hash = "hash",
            r = "10",
            s = "1",
            v = "27"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.RejectTransaction>(MockUtils.any())).willReturn(rejectTransactionJsonAdapter)
        given(rejectTransactionJsonAdapter.toJson(MockUtils.any())).willThrow(exception)

        pushServiceRepository.propagateTransactionRejected(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            signature = TEST_SIGNATURE
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RejectTransaction::class.java)
        then(rejectTransactionJsonAdapter).should().toJson(message)
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(rejectTransactionJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun requestConfirmations() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.RequestConfirmation(
            hash = "hash",
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            to = Solidity.Address(12.toBigInteger()).asEthereumAddressChecksumString(),
            value = "0",
            data = "",
            operation = "0",
            txGas = "10",
            dataGas = "10",
            operationalGas = "10",
            gasPrice = "10",
            gasToken = "0",
            refundReceiver = "0",
            nonce = "0"
        )
        given(moshiMock.adapter<ServiceMessage.RequestConfirmation>(MockUtils.any())).willReturn(requestConfirmationJsonAdapter)
        given(requestConfirmationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.requestConfirmations(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            transaction =
            SafeTransaction(
                wrapped = Transaction(address = Solidity.Address(12.toBigInteger())),
                operation = TransactionExecutionRepository.Operation.CALL
            ),
            safeAddress = TEST_SAFE_ADDRESS,
            txGas = 10.toBigInteger(),
            dataGas = 10.toBigInteger(),
            operationalGas = 10.toBigInteger(),
            gasPrice = 10.toBigInteger()
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RequestConfirmation::class.java)
        then(requestConfirmationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressString()),
                message = json,
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE)
            )
        )
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(requestConfirmationJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertNoErrors().assertComplete()
    }

    @Test
    fun requestConfirmationsErrorNotifying() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.RequestConfirmation(
            hash = "hash",
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            to = Solidity.Address(12.toBigInteger()).asEthereumAddressChecksumString(),
            value = "0",
            data = "",
            operation = "0",
            txGas = "10",
            dataGas = "10",
            operationalGas = "10",
            gasPrice = "10",
            gasToken = "0",
            refundReceiver = "0",
            nonce = "0"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.RequestConfirmation>(MockUtils.any())).willReturn(requestConfirmationJsonAdapter)
        given(requestConfirmationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.error(exception))

        pushServiceRepository.requestConfirmations(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            transaction =
            SafeTransaction(
                wrapped = Transaction(address = Solidity.Address(12.toBigInteger())),
                operation = TransactionExecutionRepository.Operation.CALL
            ),
            safeAddress = TEST_SAFE_ADDRESS,
            txGas = 10.toBigInteger(),
            dataGas = 10.toBigInteger(),
            operationalGas = 10.toBigInteger(),
            gasPrice = 10.toBigInteger()
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RequestConfirmation::class.java)
        then(requestConfirmationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressString()),
                message = json,
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE)
            )
        )
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(requestConfirmationJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun requestConfirmationsErrorSigning() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.RequestConfirmation(
            hash = "hash",
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            to = Solidity.Address(12.toBigInteger()).asEthereumAddressChecksumString(),
            value = "0",
            data = "",
            operation = "0",
            txGas = "10",
            dataGas = "10",
            operationalGas = "10",
            gasPrice = "10",
            gasToken = "0",
            refundReceiver = "0",
            nonce = "0"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.RequestConfirmation>(MockUtils.any())).willReturn(requestConfirmationJsonAdapter)
        given(requestConfirmationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.requestConfirmations(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            transaction =
            SafeTransaction(
                wrapped = Transaction(address = Solidity.Address(12.toBigInteger())),
                operation = TransactionExecutionRepository.Operation.CALL
            ),
            safeAddress = TEST_SAFE_ADDRESS,
            txGas = 10.toBigInteger(),
            dataGas = 10.toBigInteger(),
            operationalGas = 10.toBigInteger(),
            gasPrice = 10.toBigInteger()
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RequestConfirmation::class.java)
        then(requestConfirmationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(requestConfirmationJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun requestConfirmationsErrorParsing() {
        val testObserver = TestObserver.create<Unit>()
        val message = ServiceMessage.RequestConfirmation(
            hash = "hash",
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            to = Solidity.Address(12.toBigInteger()).asEthereumAddressChecksumString(),
            value = "0",
            data = "",
            operation = "0",
            txGas = "10",
            dataGas = "10",
            operationalGas = "10",
            gasPrice = "10",
            gasToken = "0",
            refundReceiver = "0",
            nonce = "0"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.RequestConfirmation>(MockUtils.any())).willReturn(requestConfirmationJsonAdapter)
        given(requestConfirmationJsonAdapter.toJson(MockUtils.any())).willThrow(exception)

        pushServiceRepository.requestConfirmations(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            transaction =
            SafeTransaction(
                wrapped = Transaction(address = Solidity.Address(12.toBigInteger())),
                operation = TransactionExecutionRepository.Operation.CALL
            ),
            safeAddress = TEST_SAFE_ADDRESS,
            txGas = 10.toBigInteger(),
            dataGas = 10.toBigInteger(),
            operationalGas = 10.toBigInteger(),
            gasPrice = 10.toBigInteger()
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RequestConfirmation::class.java)
        then(requestConfirmationJsonAdapter).should().toJson(message)
        then(moshiMock).shouldHaveNoMoreInteractions()
        then(requestConfirmationJsonAdapter).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun handlePushMessageSendTransaction() {
        val pushMessage = PushMessage.SendTransaction(
            hash = "hash",
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            to = TEST_ACCOUNT.address.asEthereumAddressString(),
            value = "",
            data = "",
            operation = "0",
            txGas = "10",
            dataGas = "10",
            operationalGas = "10",
            gasPrice = "10",
            gasToken = "0",
            nonce = "0",
            r = "12",
            s = "12",
            v = "27"
        )
        contextMock.mockGetString()
        willDoNothing().given(localNotificationManagerMock).show(anyInt(), anyString(), anyString(), MockUtils.any())

        pushServiceRepository.handlePushMessage(pushMessage)

        then(localNotificationManagerMock).should().show(
            id = eq(pushMessage.hash.hashCode()),
            title = capture(stringsArgumentCaptor),
            message = capture(stringsArgumentCaptor),
            intent = MockUtils.any()
        )

        assertEquals(R.string.sign_transaction_request_title.toString(), stringsArgumentCaptor.allValues[0])
        assertEquals(R.string.sign_transaction_request_message.toString(), stringsArgumentCaptor.allValues[1])
    }

    @Test
    fun handlePushMessageConfirmTransaction() {
        val pushMessage = PushMessage.ConfirmTransaction(
            hash = "0x42",
            r = "12",
            s = "12",
            v = "27"
        )
        val testObserver = TestObserver.create<PushServiceRepository.TransactionResponse>()

        pushServiceRepository.observe(pushMessage.hash).subscribe(testObserver)
        pushServiceRepository.handlePushMessage(pushMessage)

        testObserver.assertValue(
            PushServiceRepository.TransactionResponse.Confirmed(
                Signature(
                    r = pushMessage.r.toBigInteger(),
                    s = pushMessage.s.toBigInteger(),
                    v = pushMessage.v.toByte()
                )
            )
        )

        // Disposing one observer should remove the hash entry
        // So it wont' receive any more values
        testObserver.dispose()
        pushServiceRepository.handlePushMessage(pushMessage)
        testObserver.assertValueCount(1)
    }

    @Test
    fun handlePushMessageRejectTransaction() {
        val pushMessage = PushMessage.RejectTransaction(
            hash = "0x42",
            r = "12",
            s = "12",
            v = "27"
        )
        val testObserver = TestObserver.create<PushServiceRepository.TransactionResponse>()

        pushServiceRepository.observe(pushMessage.hash).subscribe(testObserver)
        pushServiceRepository.handlePushMessage(pushMessage)

        testObserver.assertValue(
            PushServiceRepository.TransactionResponse.Rejected(
                Signature(
                    r = pushMessage.r.toBigInteger(),
                    s = pushMessage.s.toBigInteger(),
                    v = pushMessage.v.toByte()
                )
            )
        )

        // Disposing one observer should remove the hash entry
        // So it wont' receive any more values
        testObserver.dispose()
        pushServiceRepository.handlePushMessage(pushMessage)
        testObserver.assertValueCount(1)
    }

    @Test
    fun calculateRejectionHash() {
        val testObserver = TestObserver.create<ByteArray>()
        val hash0 = "0xfff".toByteArray()
        pushServiceRepository.calculateRejectionHash(hash0).subscribe(testObserver)

        Arrays.equals(
            Sha3Utils.keccak("$SIGNATURE_PREFIX}0xfffrejectTransaction".toByteArray()),
            testObserver.values()[0]
        )

        val hash1 = "0xaaafff".toByteArray()
        pushServiceRepository.calculateRejectionHash(hash1).subscribe(testObserver)

        Arrays.equals(
            Sha3Utils.keccak("$SIGNATURE_PREFIX}0xaaafffrejectTransaction".toByteArray()),
            testObserver.values()[0]
        )

        val hash2 = "c".toByteArray()
        pushServiceRepository.calculateRejectionHash(hash2).subscribe(testObserver)

        Arrays.equals(
            Sha3Utils.keccak("$SIGNATURE_PREFIX}0xcrejectTransaction".toByteArray()),
            testObserver.values()[0]
        )
    }

    companion object {
        private val TEST_ACCOUNT = Account("0x42".asEthereumAddress()!!)
        private val TEST_SAFE_ADDRESS = "0x40".asEthereumAddress()!!
        private val TEST_EXTENSION_ADDRESS = "0x43".asEthereumAddress()!!
        private val TEST_ACCOUNT_SIGNATURE = Signature(
            r = BigInteger.ONE,
            s = BigInteger.TEN,
            v = 27.toByte()
        )
        private val TEST_SIGNATURE = Signature(
            r = BigInteger.TEN,
            s = BigInteger.ONE,
            v = 27.toByte()
        )
        private const val LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY = "prefs.string.accounttoken"
        private const val SIGNATURE_PREFIX = "GNO"
    }

    private class TestFirebaseInstanceIdTask : Task<InstanceIdResult>() {
        private val successListeners = mutableListOf<OnSuccessListener<in InstanceIdResult>>()
        private val failureListeners = mutableListOf<OnFailureListener>()

        private var complete = false
        private var exception: Exception? = null
        private var result: InstanceIdResult? = null

        override fun isComplete() = complete

        override fun getException(): Exception? = exception

        override fun addOnFailureListener(p0: OnFailureListener): Task<InstanceIdResult> {
            failureListeners.add(p0)
            return this
        }

        override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<InstanceIdResult> {
            failureListeners.add(p1)
            return this
        }

        override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<InstanceIdResult> {
            failureListeners.add(p1)
            return this
        }

        override fun getResult(): InstanceIdResult = result!!

        override fun <X : Throwable?> getResult(p0: Class<X>): InstanceIdResult = result!!

        override fun addOnSuccessListener(p0: OnSuccessListener<in InstanceIdResult>): Task<InstanceIdResult> {
            successListeners.add(p0)
            return this
        }

        override fun addOnSuccessListener(p0: Executor, p1: OnSuccessListener<in InstanceIdResult>): Task<InstanceIdResult> {
            successListeners.add(p1)
            return this
        }

        override fun addOnSuccessListener(p0: Activity, p1: OnSuccessListener<in InstanceIdResult>): Task<InstanceIdResult> {
            successListeners.add(p1)
            return this
        }

        override fun isSuccessful(): Boolean = result != null && exception == null

        override fun isCanceled(): Boolean = false

        fun setSuccess(result: TestInstanceIdResult) {
            this.result = result
            this.complete = true
            successListeners.forEach { it.onSuccess(result) }
        }

        fun setFailure(exception: Exception) {
            this.exception = exception
            this.complete = true
            failureListeners.forEach { it.onFailure(exception) }
        }
    }

    private class TestInstanceIdResult : InstanceIdResult {
        override fun getId() = "testId"
        override fun getToken() = "testToken"
    }
}

fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
