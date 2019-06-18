package pm.gnosis.heimdall.ui.walletconnect.sessions

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import javax.inject.Inject

class WalletConnectSessionsViewModel @Inject constructor(
    private val bridgeRepository: BridgeRepository
) : WalletConnectSessionsContract() {

    private lateinit var safe: Solidity.Address

    override fun setup(safe: Solidity.Address) {
        this.safe = safe
    }

    override fun observeSessions(): Observable<Adapter.Data<BridgeRepository.SessionMeta>> =
        bridgeRepository.observeSessions(safe)
            .scanToAdapterData(idExtractor = { it.id })

    override fun observeSession(sessionId: String): Observable<BridgeRepository.SessionEvent> =
        bridgeRepository.observeSession(sessionId)

    override fun createSession(url: String): Completable =
        Single.fromCallable {
            bridgeRepository.createSession(url, safe)
        }.flatMapCompletable {
            bridgeRepository.initSession(it)
        }

    override fun activateSession(sessionId: String): Completable =
        bridgeRepository.activateSession(sessionId).andThen(bridgeRepository.initSession(sessionId))

    override fun killSession(sessionId: String): Completable =
        bridgeRepository.closeSession(sessionId)

}
