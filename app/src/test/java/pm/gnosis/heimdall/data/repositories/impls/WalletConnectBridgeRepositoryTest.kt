package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.impls.wc.WCSessionStore
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.lang.RuntimeException
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
    lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    lateinit var sessionStoreMock: WCSessionStore

    @Mock
    lateinit var sessionPayloadAdapterMock: Session.PayloadAdapter

    @Mock
    lateinit var sessionTransportBuilderMock: Session.Transport.Builder

    @Mock
    lateinit var executionRepositoryMock: TransactionExecutionRepository

    private lateinit var repository: WalletConnectBridgeRepository

    @Before
    fun setUp() {
        repository = WalletConnectBridgeRepository(
            contextMock, infoRepositoryMock, localNotificationManagerMock, safeRepositoryMock,
            sessionStoreMock, sessionPayloadAdapterMock, sessionTransportBuilderMock,
            executionRepositoryMock
        )
        then(executionRepositoryMock).should().addTransactionEventsCallback(repository)
        then(executionRepositoryMock).shouldHaveNoMoreInteractions()
    }

    // TODO test for active session (sessions.containsKey(it.config.handshakeTopic))
    @Test
    fun sessions() {
        val testObserver = TestObserver<List<BridgeRepository.SessionMeta>>()
        val firstSession = WCSessionStore.State(randomConfig(), randomClientData(), null, null, UUID.randomUUID().toString(), null, null)
        val secondSession = WCSessionStore.State(
            randomConfig(),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            null,
            listOf("0xb3a4Bc89d8517E0e2C9B66703d09D3029ffa1e6d")
        )
        given(sessionStoreMock.list()).willReturn(
            listOf(
                firstSession,
                secondSession
            )
        )
        repository.sessions().subscribe(testObserver)
        then(sessionStoreMock).should().list()
        then(sessionStoreMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(
            listOf(
                BridgeRepository.SessionMeta(firstSession.config.handshakeTopic, null, null, null, false, null),
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

    // TODO test for active session (sessions.containsKey(it.config.handshakeTopic))
    @Test
    fun session() {
        val testObserver = TestObserver<BridgeRepository.SessionMeta>()
        val sessionId = UUID.randomUUID().toString()
        val session = WCSessionStore.State(
            randomConfig(sessionId),
            randomClientData(),
            randomClientData("Peer"),
            5,
            UUID.randomUUID().toString(),
            null,
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
        Session.PayloadAdapter.PeerData(UUID.randomUUID().toString(), Session.PayloadAdapter.PeerMeta(name = name))
}

