package pm.gnosis.heimdall.data.repositories.impls

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
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
import org.mockito.*
import org.mockito.BDDMockito.*
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.GnosisSafeDao
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.preferences.PreferencesSafe
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.models.push.*
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.*
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
    private lateinit var applicationMock: Application

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
    private lateinit var dbMock: ApplicationDb

    @Mock
    private lateinit var safeDaoMock: GnosisSafeDao

    @Mock
    private lateinit var cryptoHelperMock: CryptoHelper

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

    private val encryptedByteArrayConverter = EncryptedByteArray.Converter()

    private lateinit var pushServiceRepository: DefaultPushServiceRepository

    private lateinit var safePrefs: PreferencesSafe

    @Before
    fun setUp() {
        BDDMockito.given(applicationMock.getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).willReturn(testPreferences)
        safePrefs = PreferencesSafe(applicationMock)

        given(dbMock.gnosisSafeDao()).willReturn(safeDaoMock)

        pushServiceRepository = DefaultPushServiceRepository(
            contextMock,
            dbMock,
            accountsRepositoryMock,
            cryptoHelperMock,
            firebaseInstanceIdMock,
            localNotificationManagerMock,
            moshiMock,
            safePrefs,
            pushServiceApiMock
        )
    }

    @Test
    fun syncAuthenticationNewToken() {
        val owner1 = AccountsRepository.SafeOwner("0x1".asEthereumAddress()!!, encryptedByteArrayConverter.fromStorage("owner1_pk"))
        val owner2 = AccountsRepository.SafeOwner("0x2".asEthereumAddress()!!, encryptedByteArrayConverter.fromStorage("owner2_pk"))
        val safeOwners = listOf(owner1, owner2)
        val testFirebaseInstanceIdTask = TestFirebaseInstanceIdTask()
        given(firebaseInstanceIdMock.instanceId).willReturn(testFirebaseInstanceIdTask)
        given(accountsRepositoryMock.owners()).willReturn(Single.just(safeOwners))
        given(accountsRepositoryMock.sign(MockUtils.eq(owner1), MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE))
        given(accountsRepositoryMock.sign(MockUtils.eq(owner2), MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE.copy(v = 28)))
        given(pushServiceApiMock.auth(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.syncAuthentication()
        testFirebaseInstanceIdTask.setSuccess(TestInstanceIdResult())

        val hash = Sha3Utils.keccak(bundlePushInfo(testFirebaseInstanceIdTask.result.token).toByteArray())
        then(accountsRepositoryMock).should().owners()
        then(accountsRepositoryMock).should().sign(owner1, hash)
        then(accountsRepositoryMock).should().sign(owner2, hash)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(
            bundlePushInfo(testFirebaseInstanceIdTask.result.token),
            safePrefs.lastSyncedData
        )
        then(firebaseInstanceIdMock).should().instanceId
        then(firebaseInstanceIdMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).should().auth(
            PushServiceAuth(
                pushToken = testFirebaseInstanceIdTask.result.token,
                buildNumber = BuildConfig.VERSION_CODE,
                bundle = BuildConfig.APPLICATION_ID,
                versionName = BuildConfig.VERSION_NAME,
                client = "android",
                signatures = listOf(ServiceSignature.fromSignature(TEST_SIGNATURE), ServiceSignature.fromSignature(TEST_SIGNATURE.copy(v = 28)))
            )
        )
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(safeDaoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun syncAuthenticationSameToken() {
        val testFirebaseInstanceIdTask = TestFirebaseInstanceIdTask()
        given(firebaseInstanceIdMock.instanceId).willReturn(testFirebaseInstanceIdTask)

        // Set last synced token (same as new one)
        val testInstanceIdResult = TestInstanceIdResult()
        testPreferences.putString(
            LAST_SYNC_PUSH_INFO_PREFS_KEY,
            bundlePushInfo(testInstanceIdResult.token)
        )
        pushServiceRepository.syncAuthentication()
        testFirebaseInstanceIdTask.setSuccess(testInstanceIdResult)

        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(firebaseInstanceIdMock).should().instanceId
        then(firebaseInstanceIdMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(safeDaoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun syncAuthenticationForced() {
        val owner = AccountsRepository.SafeOwner("0xbaddad".asEthereumAddress()!!, encryptedByteArrayConverter.fromStorage("owner_pk"))
        val testFirebaseInstanceIdTask = TestFirebaseInstanceIdTask()
        given(firebaseInstanceIdMock.instanceId).willReturn(testFirebaseInstanceIdTask)
        given(accountsRepositoryMock.owners()).willReturn(Single.just(listOf(owner)))
        given(accountsRepositoryMock.sign(MockUtils.any<AccountsRepository.SafeOwner>(), MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE))
        given(pushServiceApiMock.auth(MockUtils.any())).willReturn(Completable.complete())

        // Set last synced token (same as new one)
        val testInstanceIdResult = TestInstanceIdResult()
        val storedBundle = bundlePushInfo(testInstanceIdResult.token)
        testPreferences.putString(LAST_SYNC_PUSH_INFO_PREFS_KEY, storedBundle)
        pushServiceRepository.syncAuthentication(forced = true)
        testFirebaseInstanceIdTask.setSuccess(testInstanceIdResult)

        then(accountsRepositoryMock).should().owners()
        val hash = Sha3Utils.keccak(bundlePushInfo(testFirebaseInstanceIdTask.result.token).toByteArray())
        then(accountsRepositoryMock).should().sign(owner, hash)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(storedBundle, testPreferences.getString(LAST_SYNC_PUSH_INFO_PREFS_KEY, ""))
        then(firebaseInstanceIdMock).should().instanceId
        then(firebaseInstanceIdMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).should().auth(
            PushServiceAuth(
                pushToken = testFirebaseInstanceIdTask.result.token,
                buildNumber = BuildConfig.VERSION_CODE,
                bundle = BuildConfig.APPLICATION_ID,
                versionName = BuildConfig.VERSION_NAME,
                client = "android",
                signatures = listOf(
                    ServiceSignature.fromSignature(TEST_SIGNATURE)
                )
            )
        )
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(safeDaoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun syncAuthenticationInstanceIdError() {
        val testFirebaseInstanceIdTask = TestFirebaseInstanceIdTask()
        val exception = Exception()
        given(firebaseInstanceIdMock.instanceId).willReturn(testFirebaseInstanceIdTask)

        // Set last synced token
        val testInstanceIdResult = TestInstanceIdResult()
        testPreferences.putString(
            LAST_SYNC_PUSH_INFO_PREFS_KEY,
            bundlePushInfo(testInstanceIdResult.token)
        )
        pushServiceRepository.syncAuthentication()
        testFirebaseInstanceIdTask.setFailure(exception)

        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        // If the FirebaseInstanceId fails then the stored token should not change
        assertEquals(
            bundlePushInfo(testInstanceIdResult.token),
            testPreferences.getString(LAST_SYNC_PUSH_INFO_PREFS_KEY, "")
        )
        then(firebaseInstanceIdMock).should().instanceId
        then(firebaseInstanceIdMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
    }

    @Test
    fun pairExistingSafe() {
        Mockito.verify(applicationMock, times(1)).getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())

        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(TEST_EXTENSION_ADDRESS)

        val safeOwner = AccountsRepository.SafeOwner(TEST_ACCOUNT_ADDRESS, encryptedByteArrayConverter.fromStorage("owner_pk"))
        given(accountsRepositoryMock.signingOwner(MockUtils.any())).willReturn(Single.just(safeOwner))
        given(accountsRepositoryMock.sign(MockUtils.any<AccountsRepository.SafeOwner>(), MockUtils.any()))
            .willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))

        given(pushServiceApiMock.pair(MockUtils.any())).willReturn(Completable.complete())

        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        val testObserver = TestObserver<Pair<AccountsRepository.SafeOwner, Solidity.Address>>()
        pushServiceRepository.pair(pushServiceTemporaryAuthorization, TEST_SAFE_ADDRESS).subscribe(testObserver)

        then(cryptoHelperMock).should().recover(
            hash = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()

        val hash = Sha3Utils.keccak("$SIGNATURE_PREFIX${TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()}".toByteArray())
        then(accountsRepositoryMock).should().signingOwner(TEST_SAFE_ADDRESS)
        then(accountsRepositoryMock).should().sign(safeOwner, hash)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()

        then(pushServiceApiMock).should().pair(
            PushServicePairing(
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE),
                temporaryAuthorization = pushServiceTemporaryAuthorization
            )
        )
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(applicationMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertResult(safeOwner to TEST_EXTENSION_ADDRESS)
    }

    @Test
    fun pairNewSafeOwner() {
        Mockito.verify(applicationMock, times(1)).getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())

        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(TEST_EXTENSION_ADDRESS)

        val newAccountAddress = "0x2323".asEthereumAddress()!!
        val safeOwner = AccountsRepository.SafeOwner(newAccountAddress, encryptedByteArrayConverter.fromStorage("new_owner_pk"))
        given(accountsRepositoryMock.createOwner()).willReturn(Single.just(safeOwner))
        given(accountsRepositoryMock.sign(MockUtils.any<AccountsRepository.SafeOwner>(), MockUtils.any()))
            .willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))

        given(pushServiceApiMock.pair(MockUtils.any())).willReturn(Completable.complete())

        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        val testObserver = TestObserver<Pair<AccountsRepository.SafeOwner, Solidity.Address>>()
        pushServiceRepository.pair(pushServiceTemporaryAuthorization, null).subscribe(testObserver)

        then(cryptoHelperMock).should().recover(
            hash = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()

        then(pushServiceApiMock).should().pair(
            PushServicePairing(
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE),
                temporaryAuthorization = pushServiceTemporaryAuthorization
            )
        )
        val hash = Sha3Utils.keccak("$SIGNATURE_PREFIX${TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()}".toByteArray())
        then(accountsRepositoryMock).should().createOwner()
        then(accountsRepositoryMock).should().sign(safeOwner, hash)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()
        then(applicationMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertResult(safeOwner to TEST_EXTENSION_ADDRESS)
    }

    @Test
    fun pairPushServiceApiError() {
        Mockito.verify(applicationMock, times(1)).getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())

        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(TEST_EXTENSION_ADDRESS)

        val safeOwner = AccountsRepository.SafeOwner(TEST_ACCOUNT_ADDRESS, encryptedByteArrayConverter.fromStorage("owner_pk"))
        given(accountsRepositoryMock.signingOwner(MockUtils.any())).willReturn(Single.just(safeOwner))
        given(accountsRepositoryMock.sign(MockUtils.any<AccountsRepository.SafeOwner>(), MockUtils.any()))
            .willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))

        val exception = IllegalStateException()
        given(pushServiceApiMock.pair(MockUtils.any())).willReturn(Completable.error(exception))

        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        val testObserver = TestObserver<Pair<AccountsRepository.SafeOwner, Solidity.Address>>()
        pushServiceRepository.pair(pushServiceTemporaryAuthorization, TEST_SAFE_ADDRESS).subscribe(testObserver)

        then(cryptoHelperMock).should().recover(
            hash = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()

        then(accountsRepositoryMock).should()
            .sign(safeOwner, Sha3Utils.keccak("$SIGNATURE_PREFIX${TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()}".toByteArray()))
        then(accountsRepositoryMock).should().signingOwner(TEST_SAFE_ADDRESS)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()

        then(pushServiceApiMock).should().pair(
            PushServicePairing(
                signature = ServiceSignature.fromSignature(TEST_ACCOUNT_SIGNATURE),
                temporaryAuthorization = pushServiceTemporaryAuthorization
            )
        )
        then(pushServiceApiMock).shouldHaveNoMoreInteractions()

        then(applicationMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun pairSignError() {
        Mockito.verify(applicationMock, times(1)).getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())

        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(TEST_EXTENSION_ADDRESS)

        val safeOwner = AccountsRepository.SafeOwner(TEST_ACCOUNT_ADDRESS, encryptedByteArrayConverter.fromStorage("owner_pk"))
        given(accountsRepositoryMock.signingOwner(MockUtils.any())).willReturn(Single.just(safeOwner))
        val exception = IllegalStateException()
        given(accountsRepositoryMock.sign(MockUtils.any<AccountsRepository.SafeOwner>(), MockUtils.any())).willReturn(Single.error(exception))

        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        val testObserver = TestObserver<Pair<AccountsRepository.SafeOwner, Solidity.Address>>()
        pushServiceRepository.pair(pushServiceTemporaryAuthorization, TEST_SAFE_ADDRESS).subscribe(testObserver)

        then(cryptoHelperMock).should().recover(
            hash = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()

        then(accountsRepositoryMock).should()
            .sign(safeOwner, Sha3Utils.keccak("$SIGNATURE_PREFIX${TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()}".toByteArray()))
        then(accountsRepositoryMock).should().signingOwner(TEST_SAFE_ADDRESS)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()

        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(applicationMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun pairRecoverError() {
        Mockito.verify(applicationMock, times(1)).getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())

        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(TEST_EXTENSION_ADDRESS)
        val exception = IllegalStateException()
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willThrow(exception)

        val pushServiceTemporaryAuthorization = PushServiceTemporaryAuthorization(
            signature = ServiceSignature.fromSignature(TEST_SIGNATURE),
            expirationDate = "testExpirationDate"
        )
        val testObserver = TestObserver<Pair<AccountsRepository.SafeOwner, Solidity.Address>>()
        pushServiceRepository.pair(pushServiceTemporaryAuthorization, TEST_SAFE_ADDRESS).subscribe(testObserver)

        then(cryptoHelperMock).should().recover(
            hash = Sha3Utils.keccak("$SIGNATURE_PREFIX${pushServiceTemporaryAuthorization.expirationDate}".toByteArray()),
            signature = pushServiceTemporaryAuthorization.signature.toSignature()
        )
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceApiMock).shouldHaveZeroInteractions()
        then(applicationMock).shouldHaveZeroInteractions()
        then(localNotificationManagerMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun propagateSafeCreation() {
        val testObserver = TestObserver.create<Unit>()
        val json = "{}"
        val message = ServiceMessage.SafeCreation(
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            owners = TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()
        )
        given(moshiMock.adapter<ServiceMessage.SafeCreation>(MockUtils.any())).willReturn(safeCreationJsonAdapter)
        given(safeCreationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.propagateSafeCreation(
            safeAddress = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SafeCreation::class.java)
        then(safeCreationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
        val message = ServiceMessage.SafeCreation(
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            owners = "${TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()},${TEST_ACCOUNT_2_ADDRESS.asEthereumAddressChecksumString()}"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.SafeCreation>(MockUtils.any())).willReturn(safeCreationJsonAdapter)
        given(safeCreationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.error(exception))

        pushServiceRepository.propagateSafeCreation(
            safeAddress = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS, TEST_ACCOUNT_2_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SafeCreation::class.java)
        then(safeCreationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
        then(pushServiceApiMock).should().notify(
            PushServiceNotification(
                devices = listOf(TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString(), TEST_ACCOUNT_2_ADDRESS.asEthereumAddressChecksumString()),
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
        val message = ServiceMessage.SafeCreation(
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            owners = TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.SafeCreation>(MockUtils.any())).willReturn(safeCreationJsonAdapter)
        given(safeCreationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.propagateSafeCreation(
            safeAddress = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SafeCreation::class.java)
        then(safeCreationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
        val message = ServiceMessage.SafeCreation(
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            owners = TEST_EXTENSION_ADDRESS.asEthereumAddressChecksumString()
        )
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
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.propagateSubmittedTransaction(
            hash = message.hash,
            chainHash = message.txHash,
            safe = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SendTransactionHash::class.java)
        then(sendTransactionHashJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.error(exception))

        pushServiceRepository.propagateSubmittedTransaction(
            hash = message.hash,
            chainHash = message.txHash,
            safe = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SendTransactionHash::class.java)
        then(sendTransactionHashJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.propagateSubmittedTransaction(
            hash = message.hash,
            chainHash = message.txHash,
            safe = TEST_SAFE_ADDRESS,
            targets = setOf(TEST_EXTENSION_ADDRESS)
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.SendTransactionHash::class.java)
        then(sendTransactionHashJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
            safe = TEST_SAFE_ADDRESS,
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
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.complete())

        pushServiceRepository.propagateTransactionRejected(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            safe = TEST_SAFE_ADDRESS,
            signature = TEST_SIGNATURE
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RejectTransaction::class.java)
        then(rejectTransactionJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
        given(pushServiceApiMock.notify(MockUtils.any())).willReturn(Completable.error(exception))

        pushServiceRepository.propagateTransactionRejected(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            safe = TEST_SAFE_ADDRESS,
            signature = TEST_SIGNATURE
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RejectTransaction::class.java)
        then(rejectTransactionJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.error(exception))

        pushServiceRepository.propagateTransactionRejected(
            targets = setOf(TEST_EXTENSION_ADDRESS),
            hash = "hash",
            safe = TEST_SAFE_ADDRESS,
            signature = TEST_SIGNATURE
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RejectTransaction::class.java)
        then(rejectTransactionJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
            safe = TEST_SAFE_ADDRESS,
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
            gasToken = "0x00000000000000000000000000000000DeaDBeef",
            refundReceiver = "0",
            nonce = "0"
        )
        given(moshiMock.adapter<ServiceMessage.RequestConfirmation>(MockUtils.any())).willReturn(requestConfirmationJsonAdapter)
        given(requestConfirmationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
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
            gasPrice = 10.toBigInteger(),
            gasToken = "0xdeadbeef".asEthereumAddress()!!
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RequestConfirmation::class.java)
        then(requestConfirmationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
            gasToken = "0x0000000000000000000000000000000000000000",
            refundReceiver = "0",
            nonce = "0"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.RequestConfirmation>(MockUtils.any())).willReturn(requestConfirmationJsonAdapter)
        given(requestConfirmationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.just(TEST_ACCOUNT_SIGNATURE))
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
            gasPrice = 10.toBigInteger(),
            gasToken = "0x0".asEthereumAddress()!!
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RequestConfirmation::class.java)
        then(requestConfirmationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
            gasToken = "0x0000000000000000000000000000000000000000",
            refundReceiver = "0",
            nonce = "0"
        )
        val exception = IllegalStateException()
        given(moshiMock.adapter<ServiceMessage.RequestConfirmation>(MockUtils.any())).willReturn(requestConfirmationJsonAdapter)
        given(requestConfirmationJsonAdapter.toJson(MockUtils.any())).willReturn(json)
        given(accountsRepositoryMock.sign(MockUtils.any<Solidity.Address>(), MockUtils.any())).willReturn(Single.error(exception))

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
            gasPrice = 10.toBigInteger(),
            gasToken = "0x0".asEthereumAddress()!!
        ).subscribe(testObserver)

        then(moshiMock).should().adapter(ServiceMessage.RequestConfirmation::class.java)
        then(requestConfirmationJsonAdapter).should().toJson(message)
        then(accountsRepositoryMock).should().sign(TEST_SAFE_ADDRESS, Sha3Utils.keccak("$SIGNATURE_PREFIX$json".toByteArray()))
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
            gasToken = "0x0000000000000000000000000000000000000000",
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
            gasPrice = 10.toBigInteger(),
            gasToken = "0x0".asEthereumAddress()!!
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
    fun handlePushMessageSendTransactionUnknownSafe() {
        val pushMessage = PushMessage.SendTransaction(
            hash = "hash",
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            to = TEST_ACCOUNT_ADDRESS.asEthereumAddressString(),
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

        pushServiceRepository.handlePushMessage(pushMessage)

        then(localNotificationManagerMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun handlePushMessageSendTransaction() {
        given(safeDaoMock.querySafe(TEST_SAFE_ADDRESS)).willReturn(GnosisSafeDb(TEST_SAFE_ADDRESS))
        val pushMessage = PushMessage.SendTransaction(
            hash = "hash",
            safe = TEST_SAFE_ADDRESS.asEthereumAddressString(),
            to = TEST_ACCOUNT_ADDRESS.asEthereumAddressString(),
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

        pushServiceRepository.handlePushMessage(pushMessage)

        then(localNotificationManagerMock).should().show(
            id = eq(pushMessage.hash.hashCode()),
            title = capture(stringsArgumentCaptor),
            message = capture(stringsArgumentCaptor),
            intent = MockUtils.any<Intent>(),
            channelId = isNull()
        )
        then(localNotificationManagerMock).shouldHaveNoMoreInteractions()

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
    fun handlePushMessageSafeCreation() {
        contextMock.mockGetString()
        contextMock.mockGetStringWithArgs()

        // We only care about the address
        given(safeDaoMock.queryPendingSafe(MockUtils.any()))
            .willReturn(PendingGnosisSafeDb(TEST_SAFE_ADDRESS, BigInteger.ZERO, "0x0".asEthereumAddress()!!, BigInteger.ZERO))

        val pushMessage = PushMessage.SafeCreation(
            safe = TEST_SAFE_ADDRESS.asEthereumAddressChecksumString()
        )

        pushServiceRepository.handlePushMessage(pushMessage)

        then(localNotificationManagerMock).should()
            .show(
                eq(TEST_SAFE_ADDRESS.hashCode()),
                MockUtils.eq(R.string.safe_created_notification_title.toString()),
                MockUtils.eq("${R.string.safe_created_notification_message.toString()}, ${TEST_SAFE_ADDRESS.asEthereumAddressChecksumString()}"),
                MockUtils.any<Intent>(),
                isNull()
            )
        then(localNotificationManagerMock).shouldHaveNoMoreInteractions()

        then(safeDaoMock).should().queryPendingSafe(TEST_SAFE_ADDRESS)
        then(safeDaoMock).should().pendingSafeToDeployedSafe(TEST_SAFE_ADDRESS)
        then(safeDaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun handlePushMessageSafeCreationNotPending() {

        val pushMessage = PushMessage.SafeCreation(
            safe = TEST_SAFE_ADDRESS.asEthereumAddressChecksumString()
        )

        pushServiceRepository.handlePushMessage(pushMessage)

        then(localNotificationManagerMock).shouldHaveZeroInteractions()

        then(safeDaoMock).should().queryPendingSafe(TEST_SAFE_ADDRESS)
        then(safeDaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun handlePushMessageSafeCreationCouldNotConvert() {

        // We only care about the address
        given(safeDaoMock.queryPendingSafe(MockUtils.any()))
            .willReturn(PendingGnosisSafeDb(TEST_SAFE_ADDRESS, BigInteger.ZERO, "0x0".asEthereumAddress()!!, BigInteger.ZERO))
        given(safeDaoMock.pendingSafeToDeployedSafe(MockUtils.any()))
            .willThrow(IllegalStateException())

        val pushMessage = PushMessage.SafeCreation(
            safe = TEST_SAFE_ADDRESS.asEthereumAddressChecksumString()
        )

        pushServiceRepository.handlePushMessage(pushMessage)

        then(localNotificationManagerMock).shouldHaveZeroInteractions()

        then(safeDaoMock).should().queryPendingSafe(TEST_SAFE_ADDRESS)
        then(safeDaoMock).should().pendingSafeToDeployedSafe(TEST_SAFE_ADDRESS)
        then(safeDaoMock).shouldHaveNoMoreInteractions()
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

    // sha3("GNO" + <pushToken> + <build_number> + <version_name> + <client> + <bundle>)
    private fun bundlePushInfo(pushToken: String) =
        "GNO$pushToken${BuildConfig.VERSION_CODE}${BuildConfig.VERSION_NAME}android${BuildConfig.APPLICATION_ID}"

    companion object {
        private val TEST_ACCOUNT_ADDRESS = "0x42".asEthereumAddress()!!
        private val TEST_ACCOUNT_2_ADDRESS = "0xaE32496491b53841efb51829d6f886387708F99B".asEthereumAddress()!!
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
        private const val LAST_SYNC_PUSH_INFO_PREFS_KEY = "prefs.string.accounttoken"
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
