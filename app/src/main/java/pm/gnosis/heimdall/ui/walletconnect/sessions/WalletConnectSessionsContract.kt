package pm.gnosis.heimdall.ui.walletconnect.sessions

import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity

abstract class WalletConnectSessionsContract : ViewModel() {
    abstract fun setup(safe: Solidity.Address)
    abstract fun observeSessions(): Observable<Adapter.Data<BridgeRepository.SessionMeta>>
    abstract fun observeSession(sessionId: String): Observable<BridgeRepository.SessionEvent>
    abstract fun createSession(url: String): Completable
    abstract fun activateSession(sessionId: String): Completable
    abstract fun killSession(sessionId: String): Completable
}
