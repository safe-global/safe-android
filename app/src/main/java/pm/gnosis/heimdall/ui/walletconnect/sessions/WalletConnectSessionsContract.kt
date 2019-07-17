package pm.gnosis.heimdall.ui.walletconnect.sessions

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity

abstract class WalletConnectSessionsContract : ViewModel() {
    abstract fun setup(safe: Solidity.Address)
    abstract fun observeSessions(): Observable<Adapter.Data<AdapterEntry>>
    abstract fun observeSession(sessionId: String): Observable<BridgeRepository.SessionEvent>
    abstract fun createSession(url: String): Completable
    abstract fun activateSession(sessionId: String): Completable
    abstract fun killSession(sessionId: String): Completable

    sealed class AdapterEntry(@IdRes val type: Int) {
        data class Header(val title: String): AdapterEntry(R.id.adapter_entry_header)
        data class Session(val meta: BridgeRepository.SessionMeta): AdapterEntry(R.id.adapter_entry_session)
    }
}
