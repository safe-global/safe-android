package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.model.Solidity

interface BridgeReposity {
    data class SessionMeta(
        val id: String,
        val dappName: String?,
        val dappDescription: String?,
        val dappIcons: List<String>?,
        val active: Boolean,
        val approvedSafes: List<Solidity.Address>?
    )

    sealed class SessionEvent {
        data class SessionRequest(val meta: SessionMeta) : SessionEvent()
        data class Closed(val id: String) : SessionEvent()
        data class Transaction(
            val id: Long,
            val from: String,
            val to: String,
            val nonce: String,
            val gasPrice: String,
            val gasLimit: String,
            val value: String,
            val data: String
        ) : SessionEvent()
    }

    fun sessions(): Single<List<SessionMeta>>
    fun createSession(url: String): String
    fun observeSession(sessionId: String): Observable<Any>
    fun initSession(sessionId: String): Completable
    fun approveSession(sessionId: String, safe: Solidity.Address): Completable
    fun closeSession(sessionId: String): Completable
    fun approveRequest(sessionId: String, requestId: Long, response: Any): Completable
    fun rejectRequest(sessionId: String, requestId: Long, errorCode: Long, errorMsg: String): Completable
}
