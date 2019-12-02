package pm.gnosis.heimdall.data.repositories.impls

import android.app.Application
import android.content.Context
import com.squareup.picasso.Picasso
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import org.walletconnect.Session
import org.walletconnect.impls.WCSessionStore
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.preferences.PreferencesWalletConnect
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.data.repositories.SessionIdAndSafe
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.util.*
import kotlin.NoSuchElementException

@RunWith(MockitoJUnitRunner::class)
class WalletConnectBridgeRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var applicationMock: Application

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var infoRepositoryMock: TransactionInfoRepository

    @Mock
    lateinit var localNotificationManagerMock: LocalNotificationManager

    @Mock
    lateinit var rpcProxyApiMock: RpcProxyApi

    @Mock
    lateinit var sessionStoreMock: WCSessionStore

    @Mock
    lateinit var picassoMock: Picasso

    @Mock
    lateinit var executionRepositoryMock: TransactionExecutionRepository

    @Mock
    lateinit var sessionBuilderMock: SessionBuilder

    @Captor
    private lateinit var callbackArgumentCaptor: ArgumentCaptor<Session.Callback>

    private val testPreferences = TestPreferences()

    private lateinit var repository: WalletConnectBridgeRepository

    private lateinit var wcPrefs: PreferencesWalletConnect

    @Before
    fun setUp() {
        BDDMockito.given(applicationMock.getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).willReturn(testPreferences)
        wcPrefs = PreferencesWalletConnect(applicationMock)

        testPreferences.clear()
        repository = WalletConnectBridgeRepository(
            contextMock, rpcProxyApiMock, picassoMock, infoRepositoryMock, localNotificationManagerMock,
            sessionStoreMock, sessionBuilderMock,
            wcPrefs, executionRepositoryMock
        )
        then(executionRepositoryMock).should().addTransactionEventsCallback(repository)
        then(executionRepositoryMock).shouldHaveNoMoreInteractions()

        Mockito.verify(applicationMock, BDDMockito.times(2)).getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())
        then(applicationMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun sessions() {
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)
        val firstSession = WCSessionStore.State(randomConfig(), randomClientData(), null, null, UUID.randomUUID().toString(), null, null)
        val secondSession = WCSessionStore.State(
            randomConfig(),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d"),
            null
        )
        given(sessionStoreMock.list()).willReturn(
            listOf(
                firstSession,
                secondSession
            )
        )
        // Active first session
        given(sessionStoreMock.load(firstSession.config.handshakeTopic)).willReturn(firstSession)
        repository.activateSession(firstSession.config.handshakeTopic).subscribe()
        then(sessionStoreMock).should().load(firstSession.config.handshakeTopic)
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        // List sessions
        val allSessionsObserver = TestObserver<List<BridgeRepository.SessionMeta>>()
        repository.sessions(null).subscribe(allSessionsObserver)
        then(sessionStoreMock).should().list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        allSessionsObserver.assertResult(
            listOf(
                BridgeRepository.SessionMeta(firstSession.config.handshakeTopic, null, null, null, null, true, null),
                BridgeRepository.SessionMeta(
                    secondSession.config.handshakeTopic,
                    "Peer",
                    null,
                    null,
                    null,
                    false,
                    listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!)
                )
            )
        )

        val filteredSessionsObserver = TestObserver<List<BridgeRepository.SessionMeta>>()
        wcPrefs.saveSession(secondSession.config.handshakeTopic, "0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!)
        repository.sessions("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!).subscribe(filteredSessionsObserver)
        then(sessionStoreMock).should(times(2)).list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        filteredSessionsObserver.assertResult(
            listOf(
                BridgeRepository.SessionMeta(
                    secondSession.config.handshakeTopic,
                    "Peer",
                    null,
                    null,
                    null,
                    false,
                    listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!)
                )
            )
        )

        val filteredUnapprovedSessionsObserver = TestObserver<List<BridgeRepository.SessionMeta>>()
        wcPrefs.saveSession(firstSession.config.handshakeTopic, "0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6a".asEthereumAddress()!!)
        repository.sessions("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6a".asEthereumAddress()!!).subscribe(filteredUnapprovedSessionsObserver)
        then(sessionStoreMock).should(times(3)).list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        filteredUnapprovedSessionsObserver.assertResult(
            listOf(
                BridgeRepository.SessionMeta(firstSession.config.handshakeTopic, null, null, null, null, true, null)
            )
        )

        val noSessionsObserver = TestObserver<List<BridgeRepository.SessionMeta>>()
        repository.sessions("0xdeadbeef".asEthereumAddress()!!).subscribe(noSessionsObserver)
        then(sessionStoreMock).should(times(4)).list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        noSessionsObserver.assertResult(listOf())
    }

    @Test
    fun sessionsError() {
        val testObserver = TestObserver<List<BridgeRepository.SessionMeta>>()
        val error = RuntimeException()
        given(sessionStoreMock.list()).willThrow(error)
        repository.sessions(null).subscribe(testObserver)
        then(sessionStoreMock).should().list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        testObserver.assertSubscribed().assertValueCount(0).assertError(error)
    }

    @Test
    fun sessionActive() {
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)
        val testObserver = TestObserver<BridgeRepository.SessionMeta>()
        val sessionId = UUID.randomUUID().toString()
        val session = WCSessionStore.State(
            randomConfig(sessionId),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d"),
            null
        )
        given(sessionStoreMock.load(MockUtils.any())).willReturn(session)
        repository.activateSession(sessionId).subscribe()
        then(sessionStoreMock).should().load(sessionId)
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        repository.session(sessionId).subscribe(testObserver)
        then(sessionStoreMock).should(times(2)).load(sessionId)
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(
            BridgeRepository.SessionMeta(
                sessionId,
                "Peer",
                null,
                null,
                null,
                true,
                listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!)
            )
        )
    }

    @Test
    fun sessionInactive() {
        val testObserver = TestObserver<BridgeRepository.SessionMeta>()
        val sessionId = UUID.randomUUID().toString()
        val session = WCSessionStore.State(
            randomConfig(sessionId),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d"),
            null
        )
        given(sessionStoreMock.load(MockUtils.any())).willReturn(session)
        repository.session(sessionId).subscribe(testObserver)
        then(sessionStoreMock).should().load(sessionId)
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(
            BridgeRepository.SessionMeta(
                sessionId,
                "Peer",
                null,
                null,
                null,
                false,
                listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!)
            )
        )
    }

    @Test
    fun sessionError() {
        val sessionId = UUID.randomUUID().toString()
        val testObserver = TestObserver<BridgeRepository.SessionMeta>()
        val error = RuntimeException()
        given(sessionStoreMock.load(MockUtils.any())).willThrow(error)
        repository.session(sessionId).subscribe(testObserver)
        then(sessionStoreMock).should().load(sessionId)
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        testObserver.assertSubscribed().assertValueCount(0).assertError(error)
    }

    @Test
    fun sessionNotFound() {
        val sessionId = UUID.randomUUID().toString()
        val testObserver = TestObserver<BridgeRepository.SessionMeta>()
        given(sessionStoreMock.load(MockUtils.any())).willReturn(null)
        repository.session(sessionId).subscribe(testObserver)
        then(sessionStoreMock).should().load(sessionId)
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(NoSuchElementException::class.java)
    }

    private fun randomConfig(id: String = UUID.randomUUID().toString()) =
        Session.Config(id, "wss://bridge.gnosis.pm", UUID.randomUUID().toString())

    private fun randomClientData(name: String = "Safe Tests") =
        Session.PeerData(UUID.randomUUID().toString(), Session.PeerMeta(name = name))

    @Test
    fun createSessionNew() {
        contextMock.mockGetString()
        val testSafe = "0xbaddad".asEthereumAddress()!!
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)
        assertEquals(config.handshakeTopic, repository.createSession(url, testSafe))
        assertEquals(wcPrefs.safeForSession(config.handshakeTopic), testSafe)
        then(sessionStoreMock).shouldHaveZeroInteractions()
        then(sessionBuilderMock).should().build(
            config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().startService(MockUtils.any())
        then(contextMock).should().getString(R.string.app_name)
        then(contextMock).shouldHaveNoMoreInteractions()

        // Should only create once
        assertEquals(config.handshakeTopic, repository.createSession(url, testSafe))
        assertEquals(testPreferences.all.size, 1)
        assertEquals(wcPrefs.safeForSession(config.handshakeTopic), testSafe)
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should(times(2)).startService(MockUtils.any())
        then(contextMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun createSessionAutoInit() {
        contextMock.mockGetString()
        val testSafe = "0xbaddad".asEthereumAddress()!!
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)

        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)
        assertEquals(config.handshakeTopic, repository.createSession(url, testSafe))
        assertEquals(wcPrefs.safeForSession(config.handshakeTopic), testSafe)
        then(sessionStoreMock).shouldHaveZeroInteractions()
        then(sessionBuilderMock).should().build(
            config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().startService(MockUtils.any())
        then(contextMock).should().getString(R.string.app_name)
        then(contextMock).shouldHaveNoMoreInteractions()

        then(mockSession).should().addCallback(capture(callbackArgumentCaptor))
        then(mockSession).shouldHaveNoMoreInteractions()

        val someId = System.currentTimeMillis()
        callbackArgumentCaptor.value.onMethodCall(Session.MethodCall.SessionRequest(someId, Session.PeerData(UUID.randomUUID().toString(), null)))
        then(mockSession).should().approve(listOf(testSafe.asEthereumAddressString()), BuildConfig.BLOCKCHAIN_CHAIN_ID)
        then(mockSession).shouldHaveNoMoreInteractions()
        // No safe for session should reject
        testPreferences.clear()
        callbackArgumentCaptor.value.onMethodCall(Session.MethodCall.SessionRequest(someId, Session.PeerData(UUID.randomUUID().toString(), null)))
        then(mockSession).should().reject()
        then(mockSession).shouldHaveNoMoreInteractions()


        // Should only create once
        assertEquals(config.handshakeTopic, repository.createSession(url, testSafe))
        assertEquals(testPreferences.all.size, 1)
        assertEquals(wcPrefs.safeForSession(config.handshakeTopic), testSafe)
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should(times(2)).startService(MockUtils.any())
        then(contextMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initSession() {
        contextMock.mockGetString()
        val testSafe = "0xbaddad".asEthereumAddress()!!
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)

        val failObserver = TestObserver<Unit>()
        repository.initSession("randomid").subscribe(failObserver)
        failObserver.assertFailure(Predicate<Throwable> { it is IllegalArgumentException && it.message == "Session not active" })

        val sessionId = repository.createSession(url, testSafe)
        then(sessionBuilderMock).should().build(
            config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(mockSession).should().addCallback(MockUtils.any())
        then(mockSession).shouldHaveNoMoreInteractions()
        val successObserver = TestObserver<Unit>()
        repository.initSession(sessionId).subscribe(successObserver)
        successObserver.assertResult()
        then(mockSession).should().init()
        then(mockSession).shouldHaveNoMoreInteractions()
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun closeSession() {
        contextMock.mockGetString()
        val testSafe = "0xbaddad".asEthereumAddress()!!
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)

        val failObserver = TestObserver<Unit>()
        repository.closeSession("randomid").subscribe(failObserver)
        failObserver.assertFailure(Predicate<Throwable> { it is IllegalArgumentException && it.message == "Session not active" })

        val sessionId = repository.createSession(url, testSafe)
        then(sessionBuilderMock).should().build(
            config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(mockSession).should().addCallback(MockUtils.any())
        then(mockSession).shouldHaveNoMoreInteractions()
        val successObserver = TestObserver<Unit>()
        repository.closeSession(sessionId).subscribe(successObserver)
        successObserver.assertResult()
        then(mockSession).should().kill()
        then(mockSession).shouldHaveNoMoreInteractions()
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun activateSessionNotStored() {
        val testObserver = TestObserver<Unit>()
        repository.activateSession("anyid").subscribe(testObserver)
        testObserver.assertFailure(Predicate<Throwable> { it is IllegalArgumentException && it.message == "Session not found" })
    }

    @Test
    fun activateSession() {
        contextMock.mockGetString()
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        given(sessionStoreMock.load(MockUtils.any())).willReturn(
            WCSessionStore.State(config, Session.PeerData("id", null), null, null, config.key, null, null)
        )
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)
        val createObserver = TestObserver<Unit>()
        repository.activateSession(config.handshakeTopic).subscribe(createObserver)
        createObserver.assertResult()
        then(sessionStoreMock).should().load(config.handshakeTopic)
        then(sessionStoreMock).shouldHaveZeroInteractions()
        then(sessionBuilderMock).should().build(
            config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().startService(MockUtils.any())
        then(contextMock).should().getString(R.string.app_name)
        then(contextMock).shouldHaveNoMoreInteractions()

        then(mockSession).should().addCallback(MockUtils.any())
        then(mockSession).shouldHaveNoMoreInteractions()

        // Should only create once
        val activeObserver = TestObserver<Unit>()
        repository.activateSession(config.handshakeTopic).subscribe(activeObserver)
        activeObserver.assertResult()
        then(sessionStoreMock).should(times(2)).load(config.handshakeTopic)
        then(sessionStoreMock).shouldHaveZeroInteractions()
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should(times(2)).startService(MockUtils.any())
        then(contextMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSessions() {
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)

        val observer = TestObserver<List<BridgeRepository.SessionMeta>>()
        repository.observeSessions(null).subscribe(observer)
        then(sessionStoreMock).should().list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        // Initial value should be empty (even if there is a error while loading the session from the store)
        observer.assertValues(emptyList())

        // Setup sessions
        val firstSession = WCSessionStore.State(randomConfig(), randomClientData(), null, null, UUID.randomUUID().toString(), null, null)
        val secondSession = WCSessionStore.State(
            randomConfig(),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d"),
            null
        )
        given(sessionStoreMock.list()).willReturn(
            listOf(
                firstSession,
                secondSession
            )
        )
        // Activate first session, should trigger an update
        given(sessionStoreMock.load(firstSession.config.handshakeTopic)).willReturn(firstSession)
        repository.activateSession(firstSession.config.handshakeTopic).subscribe()
        then(sessionStoreMock).should().load(firstSession.config.handshakeTopic)
        then(sessionStoreMock).should(times(2)).list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        observer.assertValues(
            emptyList(),
            listOf(
                BridgeRepository.SessionMeta(firstSession.config.handshakeTopic, null, null, null, null, true, null),
                BridgeRepository.SessionMeta(
                    secondSession.config.handshakeTopic,
                    "Peer",
                    null,
                    null,
                    null,
                    false,
                    listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!)
                )
            )
        )
    }

    @Test
    fun observeActiveSessionIds() {
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)

        val observer = TestObserver<List<SessionIdAndSafe>>()
        repository.observeActiveSessionInfo().subscribe(observer)
        // Initial value should be empty (even if there is a error while loading the session from the store)
        observer.assertValues(emptyList())

        // Setup sessions
        val firstSession = WCSessionStore.State(randomConfig(), randomClientData(), null, null, UUID.randomUUID().toString(), null, null)
        val firstSessionSafe = "0xbaddad".asEthereumAddress()!!
        wcPrefs.saveSession(firstSession.config.handshakeTopic, firstSessionSafe)
        val secondSession = WCSessionStore.State(
            randomConfig(),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d"),
            null
        )
        val secondSessionSafe = "0xdeadbeef".asEthereumAddress()!!
        wcPrefs.saveSession(secondSession.config.handshakeTopic, secondSessionSafe)

        // Activate first session, should trigger an update
        given(sessionStoreMock.load(firstSession.config.handshakeTopic)).willReturn(firstSession)
        repository.activateSession(firstSession.config.handshakeTopic).subscribe()
        then(sessionStoreMock).should().load(firstSession.config.handshakeTopic)

        observer.assertValues(emptyList(), listOf(firstSession.config.handshakeTopic to firstSessionSafe))

        // Activate second session, should trigger an update
        given(sessionStoreMock.load(secondSession.config.handshakeTopic)).willReturn(secondSession)
        repository.activateSession(secondSession.config.handshakeTopic).subscribe()
        then(sessionStoreMock).should().load(secondSession.config.handshakeTopic)

        observer.assertValues(
            emptyList(),
            listOf(firstSession.config.handshakeTopic to firstSessionSafe),
            listOf(firstSession.config.handshakeTopic to firstSessionSafe, secondSession.config.handshakeTopic to secondSessionSafe)
                .sortedBy { it.first }
        )

        then(sessionStoreMock).shouldHaveNoMoreInteractions()
    }
}

