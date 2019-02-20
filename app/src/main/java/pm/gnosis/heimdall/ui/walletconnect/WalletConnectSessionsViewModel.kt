package pm.gnosis.heimdall.ui.walletconnect

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import javax.inject.Inject

class WalletConnectSessionsViewModel @Inject constructor(
    private val bridgeRepository: BridgeRepository
) : WalletConnectSessionsContract() {

    override fun observeSessions(): Observable<Adapter.Data<BridgeRepository.SessionMeta>> =
        bridgeRepository.observeSessions().scanToAdapterData(idExtractor = { it.id })

    override fun observeSession(sessionId: String): Observable<BridgeRepository.SessionEvent> =
        bridgeRepository.observeSession(sessionId)

    override fun createSession(url: String): Completable =
        Single.fromCallable {
            bridgeRepository.createSession(url)
        }.flatMapCompletable {
            bridgeRepository.initSession(it)
        }

    override fun activateSession(sessionId: String): Completable =
        bridgeRepository.activateSession(sessionId).andThen(bridgeRepository.initSession(sessionId))

    override fun approveSession(sessionId: String): Completable =
        bridgeRepository.approveSession(sessionId)

    override fun denySession(sessionId: String): Completable =
        bridgeRepository.rejectSession(sessionId)

    override fun killSession(sessionId: String): Completable =
        bridgeRepository.closeSession(sessionId)

}
