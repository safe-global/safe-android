package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import io.reactivex.Flowable
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import org.walletconnect.Session
import org.walletconnect.impls.WCSessionStore
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import java.util.*
import kotlin.NoSuchElementException

@RunWith(MockitoJUnitRunner::class)
class WalletConnectBridgeRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var infoRepositoryMock: TransactionInfoRepository

    @Mock
    lateinit var localNotificationManagerMock: LocalNotificationManager

    @Mock
    lateinit var rpcProxyApiMock: RpcProxyApi

    @Mock
    lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    lateinit var sessionStoreMock: WCSessionStore

    @Mock
    lateinit var executionRepositoryMock: TransactionExecutionRepository

    @Mock
    lateinit var sessionBuilderMock: SessionBuilder

    private lateinit var repository: WalletConnectBridgeRepository

    @Before
    fun setUp() {
        repository = WalletConnectBridgeRepository(
            contextMock, rpcProxyApiMock, infoRepositoryMock, localNotificationManagerMock, safeRepositoryMock,
            sessionStoreMock, sessionBuilderMock,
            executionRepositoryMock
        )
        then(executionRepositoryMock).should().addTransactionEventsCallback(repository)
        then(executionRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun sessions() {
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)
        val testObserver = TestObserver<List<BridgeRepository.SessionMeta>>()
        val firstSession = WCSessionStore.State(randomConfig(), randomClientData(), null, null, UUID.randomUUID().toString(), null)
        val secondSession = WCSessionStore.State(
            randomConfig(),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d")
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
        repository.sessions().subscribe(testObserver)
        then(sessionStoreMock).should().list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(
            listOf(
                BridgeRepository.SessionMeta(firstSession.config.handshakeTopic, null, null, null, true, null),
                BridgeRepository.SessionMeta(
                    secondSession.config.handshakeTopic,
                    "Peer",
                    null,
                    null,
                    false,
                    listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!)
                )
            )
        )
    }

    @Test
    fun sessionsError() {
        val testObserver = TestObserver<List<BridgeRepository.SessionMeta>>()
        val error = RuntimeException()
        given(sessionStoreMock.list()).willThrow(error)
        repository.sessions().subscribe(testObserver)
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
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d")
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
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d")
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
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)
        assertEquals(config.handshakeTopic, repository.createSession(url))
        then(sessionStoreMock).shouldHaveZeroInteractions()
        then(sessionBuilderMock).should().build(config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().startService(MockUtils.any())
        then(contextMock).should().getString(R.string.app_name)
        then(contextMock).shouldHaveNoMoreInteractions()

        // Should only create once
        assertEquals(config.handshakeTopic, repository.createSession(url))
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should(times(2)).startService(MockUtils.any())
        then(contextMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initSession() {
        contextMock.mockGetString()
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)

        val failObserver = TestObserver<Unit>()
        repository.initSession("randomid").subscribe(failObserver)
        failObserver.assertFailure(Predicate<Throwable> { it is IllegalArgumentException && it.message == "Session not active" })

        val sessionId = repository.createSession(url)
        then(sessionBuilderMock).should().build(config,
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
    fun rejectSession() {
        contextMock.mockGetString()
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)

        val failObserver = TestObserver<Unit>()
        repository.rejectSession("randomid").subscribe(failObserver)
        failObserver.assertFailure(Predicate<Throwable> { it is IllegalArgumentException && it.message == "Session not active" })

        val sessionId = repository.createSession(url)
        then(sessionBuilderMock).should().build(config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(mockSession).should().addCallback(MockUtils.any())
        then(mockSession).shouldHaveNoMoreInteractions()
        val successObserver = TestObserver<Unit>()
        repository.rejectSession(sessionId).subscribe(successObserver)
        successObserver.assertResult()
        then(mockSession).should().reject()
        then(mockSession).shouldHaveNoMoreInteractions()
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun closeSession() {
        contextMock.mockGetString()
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)

        val failObserver = TestObserver<Unit>()
        repository.closeSession("randomid").subscribe(failObserver)
        failObserver.assertFailure(Predicate<Throwable> { it is IllegalArgumentException && it.message == "Session not active" })

        val sessionId = repository.createSession(url)
        then(sessionBuilderMock).should().build(config,
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
    fun approveSessionNoSafes() {
        given(safeRepositoryMock.observeSafes()).willReturn(Flowable.just(emptyList()))
        val failObserver = TestObserver<Unit>()
        repository.approveSession("anyid").subscribe(failObserver)
        failObserver.assertFailure(Predicate<Throwable> { it is IllegalStateException && it.message == "No Safe to whitelist" })
        then(safeRepositoryMock).should().observeSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun approveSessionNoSafesEvent() {
        given(safeRepositoryMock.observeSafes()).willReturn(Flowable.empty())
        val failObserver = TestObserver<Unit>()
        repository.approveSession("anyid").subscribe(failObserver)
        failObserver.assertFailure(NoSuchElementException::class.java)
        then(safeRepositoryMock).should().observeSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun approveSessionSafesError() {
        val error = IllegalStateException("test exception")
        given(safeRepositoryMock.observeSafes()).willReturn(Flowable.error(error))
        val failObserver = TestObserver<Unit>()
        repository.approveSession("anyid").subscribe(failObserver)
        failObserver.assertFailure(Predicate<Throwable> { it == error })
        then(safeRepositoryMock).should().observeSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun approveSession() {
        given(safeRepositoryMock.observeSafes()).willReturn(
            Flowable.just(
                listOf(
                    Safe("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d".asEthereumAddress()!!),
                    Safe("0x0e329fa8d6Fcd1ba0Cda495431F1f7CA24F442C2".asEthereumAddress()!!)
                )
            )
        )
        contextMock.mockGetString()
        val url =
            "wc:12345678-4242-4242-4242-abcdef42abcd@1?bridge=https%3A%2F%2Fbridge.gnosis.pm&key=5eef4242e18349068f7ba3dbd909b3182be28475879d24e929c60e3cbb2d36ee"
        val config = Session.Config.fromWCUri(url)
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)

        val failObserver = TestObserver<Unit>()
        repository.approveSession("randomid").subscribe(failObserver)
        failObserver.assertFailure(Predicate<Throwable> { it is IllegalArgumentException && it.message == "Session not active" })

        val sessionId = repository.createSession(url)
        then(sessionBuilderMock).should().build(config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(mockSession).should().addCallback(MockUtils.any())
        then(mockSession).shouldHaveNoMoreInteractions()
        val successObserver = TestObserver<Unit>()
        repository.approveSession(sessionId).subscribe(successObserver)
        successObserver.assertResult()
        then(mockSession).should().approve(
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d", "0x0e329fa8d6Fcd1ba0Cda495431F1f7CA24F442C2"),
            BuildConfig.BLOCKCHAIN_CHAIN_ID
        )
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
            WCSessionStore.State(config, Session.PeerData("id", null), null, null, config.key, null)
        )
        val mockSession = mock(Session::class.java)
        given(sessionBuilderMock.build(MockUtils.any(), MockUtils.any())).willReturn(mockSession)
        val createObserver = TestObserver<Unit>()
        repository.activateSession(config.handshakeTopic).subscribe(createObserver)
        createObserver.assertResult()
        then(sessionStoreMock).should().load(config.handshakeTopic)
        then(sessionStoreMock).shouldHaveZeroInteractions()
        then(sessionBuilderMock).should().build(config,
            Session.PeerMeta(name = R.string.app_name.toString())
        )
        then(sessionBuilderMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().startService(MockUtils.any())
        then(contextMock).should().getString(R.string.app_name)
        then(contextMock).shouldHaveNoMoreInteractions()

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
        repository.observeSessions().subscribe(observer)
        then(sessionStoreMock).should().list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        // Initial value should be empty (even if there is a error while loading the session from the store)
        observer.assertValues(emptyList())

        // Setup sessions
        val firstSession = WCSessionStore.State(randomConfig(), randomClientData(), null, null, UUID.randomUUID().toString(), null)
        val secondSession = WCSessionStore.State(
            randomConfig(),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d")
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
                BridgeRepository.SessionMeta(firstSession.config.handshakeTopic, null, null, null, true, null),
                BridgeRepository.SessionMeta(
                    secondSession.config.handshakeTopic,
                    "Peer",
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

        val observer = TestObserver<List<String>>()
        repository.observeActiveSessionIds().subscribe(observer)
        // Initial value should be empty (even if there is a error while loading the session from the store)
        observer.assertValues(emptyList())

        // Setup sessions
        val firstSession = WCSessionStore.State(randomConfig(), randomClientData(), null, null, UUID.randomUUID().toString(), null)
        val secondSession = WCSessionStore.State(
            randomConfig(),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d")
        )

        // Activate first session, should trigger an update
        given(sessionStoreMock.load(firstSession.config.handshakeTopic)).willReturn(firstSession)
        repository.activateSession(firstSession.config.handshakeTopic).subscribe()
        then(sessionStoreMock).should().load(firstSession.config.handshakeTopic)

        observer.assertValues(emptyList(), listOf(firstSession.config.handshakeTopic))

        // Activate second session, should trigger an update
        given(sessionStoreMock.load(secondSession.config.handshakeTopic)).willReturn(secondSession)
        repository.activateSession(secondSession.config.handshakeTopic).subscribe()
        then(sessionStoreMock).should().load(secondSession.config.handshakeTopic)

        observer.assertValues(
            emptyList(),
            listOf(firstSession.config.handshakeTopic),
            listOf(firstSession.config.handshakeTopic, secondSession.config.handshakeTopic).sorted()
        )

        then(sessionStoreMock).shouldHaveNoMoreInteractions()
    }

}

