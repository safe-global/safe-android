package pm.gnosis.heimdall.ui.walletconnect

import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.ui.base.Adapter

abstract class WalletConnectSessionsContract : ViewModel() {
    abstract fun observeSessions(): Observable<Adapter.Data<BridgeRepository.SessionMeta>>
    abstract fun observeSession(sessionId: String): Observable<BridgeRepository.SessionEvent>
    abstract fun createSession(url: String): Completable
    abstract fun activateSession(sessionId: String): Completable
    abstract fun approveSession(sessionId: String): Completable
    abstract fun denySession(sessionId: String): Completable
    abstract fun killSession(sessionId: String): Completable
}
