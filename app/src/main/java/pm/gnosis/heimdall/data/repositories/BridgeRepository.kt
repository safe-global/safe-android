package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.model.Solidity


typealias SessionIdAndSafe = Pair<String, Solidity.Address>

interface BridgeRepository {
    data class SessionMeta(
        val id: String,
        val dappName: String?,
        val dappDescription: String?,
        val dappUrl: String?,
        val dappIcons: List<String>?,
        val active: Boolean,
        val approvedSafes: List<Solidity.Address>?
    )

    sealed class SessionEvent {
        data class MetaUpdate(val meta: SessionMeta) : SessionEvent()
        data class Closed(val id: String) : SessionEvent()
        data class Transaction(
            val id: Long,
            val from: String,
            val to: String,
            val nonce: String?,
            val gasPrice: String?,
            val gasLimit: String?,
            val value: String,
            val data: String
        ) : SessionEvent()
    }

    fun sessions(safe: Solidity.Address?): Single<List<SessionMeta>>
    fun session(sessionId: String): Single<SessionMeta>
    fun createSession(url: String, safe: Solidity.Address): String
    fun observeSession(sessionId: String): Observable<SessionEvent>
    fun initSession(sessionId: String): Completable
    fun closeSession(sessionId: String): Completable
    fun activateSession(sessionId: String): Completable
    fun approveRequest(requestId: Long, response: Any): Completable
    fun rejectRequest(requestId: Long, errorCode: Long, errorMsg: String): Completable
    fun observeActiveSessionInfo(): Observable<List<SessionIdAndSafe>>
    fun observeSessions(safe: Solidity.Address?): Observable<List<SessionMeta>>
    fun shouldShowIntro(): Single<Boolean>
    fun markIntroDone(): Completable
}
