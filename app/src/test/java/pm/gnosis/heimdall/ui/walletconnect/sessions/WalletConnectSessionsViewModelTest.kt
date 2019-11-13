package pm.gnosis.heimdall.ui.walletconnect.sessions

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.svalinn.common.utils.WhatTheFuck
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import java.util.*
import java.util.concurrent.BrokenBarrierException

@RunWith(MockitoJUnitRunner::class)
class WalletConnectSessionsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var bridgeRepoMock: BridgeRepository

    private lateinit var viewModel: WalletConnectSessionsViewModel

    @Before
    fun setUp() {
        viewModel = WalletConnectSessionsViewModel(contextMock, bridgeRepoMock)
        viewModel.setup(TEST_SAFE)
    }

    @Test
    fun observeSession() {
        val eventId = UUID.randomUUID().toString()
        val sessionId = UUID.randomUUID().toString()
        given(bridgeRepoMock.observeSession(MockUtils.any())).willReturn(Observable.just(BridgeRepository.SessionEvent.Closed(eventId)))
        val testObserver = TestObserver<BridgeRepository.SessionEvent>()
        viewModel.observeSession(sessionId).subscribe(testObserver)
        testObserver.assertResult(BridgeRepository.SessionEvent.Closed(eventId))
        then(bridgeRepoMock).should().observeSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun observeSessionError() {
        val sessionId = UUID.randomUUID().toString()
        val error = WhatTheFuck(BrokenBarrierException())
        given(bridgeRepoMock.observeSession(MockUtils.any())).willReturn(Observable.error(error))
        val testObserver = TestObserver<BridgeRepository.SessionEvent>()
        viewModel.observeSession(sessionId).subscribe(testObserver)
        testObserver.assertSubscribed().assertError(error).assertNoValues()
        then(bridgeRepoMock).should().observeSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun killSession() {
        val sessionId = UUID.randomUUID().toString()
        given(bridgeRepoMock.activateSession(MockUtils.any())).willReturn(Completable.complete())
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(Completable.complete())
        given(bridgeRepoMock.closeSession(MockUtils.any())).willReturn(Completable.complete())
        val testObserver = TestObserver<Unit>()
        viewModel.killSession(sessionId).subscribe(testObserver)
        testObserver.assertComplete()
        then(bridgeRepoMock).should().activateSession(sessionId)
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).should().closeSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun killSessionError() {
        val sessionId = UUID.randomUUID().toString()
        val error = WhatTheFuck(BrokenBarrierException())
        given(bridgeRepoMock.activateSession(MockUtils.any())).willReturn(Completable.complete())
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(Completable.complete())
        given(bridgeRepoMock.closeSession(MockUtils.any())).willReturn(Completable.error(error))
        val testObserver = TestObserver<Unit>()
        viewModel.killSession(sessionId).subscribe(testObserver)
        testObserver.assertSubscribed().assertError(error).assertNoValues()
        then(bridgeRepoMock).should().activateSession(sessionId)
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).should().closeSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun killSessionErrorInit() {
        val sessionId = UUID.randomUUID().toString()
        val error = WhatTheFuck(BrokenBarrierException())
        val testCompletable = TestCompletable()
        given(bridgeRepoMock.activateSession(MockUtils.any())).willReturn(Completable.complete())
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(Completable.error(error))
        given(bridgeRepoMock.closeSession(MockUtils.any())).willReturn(testCompletable)
        val testObserver = TestObserver<Unit>()
        viewModel.killSession(sessionId).subscribe(testObserver)
        testObserver.assertSubscribed().assertError(error).assertNoValues()
        then(bridgeRepoMock).should().activateSession(sessionId)
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).should().closeSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        assertEquals(0, testCompletable.callCount)
    }

    @Test
    fun killSessionErrorActivate() {
        val sessionId = UUID.randomUUID().toString()
        val error = WhatTheFuck(BrokenBarrierException())
        val testCompletable = TestCompletable()
        given(bridgeRepoMock.activateSession(MockUtils.any())).willReturn(Completable.error(error))
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(testCompletable)
        given(bridgeRepoMock.closeSession(MockUtils.any())).willReturn(testCompletable)
        val testObserver = TestObserver<Unit>()
        viewModel.killSession(sessionId).subscribe(testObserver)
        testObserver.assertSubscribed().assertError(error).assertNoValues()
        then(bridgeRepoMock).should().activateSession(sessionId)
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).should().closeSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        assertEquals(0, testCompletable.callCount)
    }

    @Test
    fun createSession() {
        val sessionId = UUID.randomUUID().toString()
        given(bridgeRepoMock.createSession(MockUtils.any(), MockUtils.any())).willReturn(sessionId)
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(Completable.complete())
        val url = "ws:someconfigparams@kkhfksddfjsgsh"
        val testObserver = TestObserver<Unit>()
        viewModel.createSession(url).subscribe(testObserver)
        testObserver.assertComplete()
        then(bridgeRepoMock).should().createSession(url,
            TEST_SAFE
        )
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun createSessionCreationError() {
        val error = WhatTheFuck(BrokenBarrierException())
        given(bridgeRepoMock.createSession(MockUtils.any(), MockUtils.any())).willThrow(error)
        val url = "ws:someconfigparams@kkhfksddfjsgsh"
        val testObserver = TestObserver<Unit>()
        viewModel.createSession(url).subscribe(testObserver)
        testObserver.assertSubscribed().assertError(error).assertNoValues()
        then(bridgeRepoMock).should().createSession(url,
            TEST_SAFE
        )
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun createSessionInitError() {
        val sessionId = UUID.randomUUID().toString()
        val error = WhatTheFuck(BrokenBarrierException())
        given(bridgeRepoMock.createSession(MockUtils.any(), MockUtils.any())).willReturn(sessionId)
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(Completable.error(error))
        val url = "ws:someconfigparams@kkhfksddfjsgsh"
        val testObserver = TestObserver<Unit>()
        viewModel.createSession(url).subscribe(testObserver)
        testObserver.assertSubscribed().assertError(error).assertNoValues()
        then(bridgeRepoMock).should().createSession(url,
            TEST_SAFE
        )
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun activateSession() {
        val sessionId = UUID.randomUUID().toString()
        given(bridgeRepoMock.activateSession(MockUtils.any())).willReturn(Completable.complete())
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(Completable.complete())
        val testObserver = TestObserver<Unit>()
        viewModel.activateSession(sessionId).subscribe(testObserver)
        testObserver.assertComplete()
        then(bridgeRepoMock).should().activateSession(sessionId)
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun activateSessionActivateError() {
        val testCompletable = TestCompletable()
        val sessionId = UUID.randomUUID().toString()
        val error = WhatTheFuck(BrokenBarrierException())
        given(bridgeRepoMock.activateSession(MockUtils.any())).willReturn(Completable.error(error))
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(testCompletable)
        val testObserver = TestObserver<Unit>()
        viewModel.activateSession(sessionId).subscribe(testObserver)
        testObserver.assertSubscribed().assertError(error).assertNoValues()
        then(bridgeRepoMock).should().activateSession(sessionId)
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        assertEquals("Init Callable should not be triggered", 0, testCompletable.callCount)
    }

    @Test
    fun activateSessionInitError() {
        val sessionId = UUID.randomUUID().toString()
        val error = WhatTheFuck(BrokenBarrierException())
        given(bridgeRepoMock.activateSession(MockUtils.any())).willReturn(Completable.complete())
        given(bridgeRepoMock.initSession(MockUtils.any())).willReturn(Completable.error(error))
        val testObserver = TestObserver<Unit>()
        viewModel.activateSession(sessionId).subscribe(testObserver)
        testObserver.assertSubscribed().assertError(error).assertNoValues()
        then(bridgeRepoMock).should().activateSession(sessionId)
        then(bridgeRepoMock).should().initSession(sessionId)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
    }

    @Test
    fun observeSessions() {
        contextMock.mockGetString()
        val metaSubject = PublishSubject.create<List<BridgeRepository.SessionMeta>>()
        given(bridgeRepoMock.observeSessions(MockUtils.any())).willReturn(metaSubject)
        val testObserver = TestObserver<Adapter.Data<WalletConnectSessionsContract.AdapterEntry>>()
        viewModel.observeSessions().subscribe(testObserver)
        then(bridgeRepoMock).should().observeSessions(TEST_SAFE)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveNoMoreInteractions()
        testObserver
            .assertValueCount(1)
            .assertValueAt(0) {
                it.parentId == null &&
                        it.diff == null &&
                        it.entries == emptyList<WalletConnectSessionsContract.AdapterEntry>()
            }
        metaSubject.onNext(emptyList())
        testObserver
            .assertValueCount(2)
            .assertValueAt(1) {
                it.parentId == testObserver.values()[0].id &&
                        it.diff != null &&
                        it.entries == emptyList<WalletConnectSessionsContract.AdapterEntry>()
            }
        val initialValues = listOf(BridgeRepository.SessionMeta(UUID.randomUUID().toString(), null, null, null, null, false, null))
        metaSubject.onNext(initialValues)
        then(contextMock).should().getString(R.string.active_sessions)
        then(contextMock).shouldHaveNoMoreInteractions()
        testObserver
            .assertValueCount(3)
            .assertValueAt(2) {
                it.parentId == testObserver.values()[1].id &&
                        it.diff != null &&
                        it.entries == toAdapterEntries(initialValues)
            }
        val updatedValues = listOf(
            initialValues.first(),
            BridgeRepository.SessionMeta(UUID.randomUUID().toString(), null, null, null, null, false, null)
        )
        metaSubject.onNext(updatedValues)
        then(contextMock).should(times(2)).getString(R.string.active_sessions)
        then(contextMock).shouldHaveNoMoreInteractions()
        testObserver
            .assertValueCount(4)
            .assertValueAt(3) {
                it.parentId == testObserver.values()[2].id &&
                        it.diff != null &&
                        it.entries == toAdapterEntries(updatedValues)
            }
        val error = WhatTheFuck(BrokenBarrierException())
        metaSubject.onError(error)
        testObserver
            .assertValueCount(4)
            .assertError(error)
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
    }

    private fun toAdapterEntries(entries: List<BridgeRepository.SessionMeta>) =
            listOf(WalletConnectSessionsContract.AdapterEntry.Header(R.string.active_sessions.toString())) +
                    entries.map { WalletConnectSessionsContract.AdapterEntry.Session(it) }

    companion object {
        private val TEST_SAFE = "0xbaddad".asEthereumAddress()!!
    }
}
