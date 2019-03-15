package pm.gnosis.heimdall.data.repositories.impls

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.impls.wc.*
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.heimdall.services.BridgeService
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigIntegerOrNull
import java.lang.IllegalStateException
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

class WalletConnectBridgeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val infoRepository: TransactionInfoRepository,
    private val localNotificationManager: LocalNotificationManager,
    private val safeRepository: GnosisSafeRepository,
    private val sessionStore: WCSessionStore,
    private val sessionBuilder: SessionBuilder,
    executionRepository: TransactionExecutionRepository
) : BridgeRepository, TransactionExecutionRepository.TransactionEventsCallback {
    private val sessionUpdates = BehaviorSubject.createDefault(Unit)
    private val sessions: MutableMap<String, Session> = ConcurrentHashMap()

    private val sessionRequests: MutableMap<Long, String> = ConcurrentHashMap()

    init {
        executionRepository.addTransactionEventsCallback(this)
    }

    override fun onTransactionRejected(referenceId: Long) {
        sessionForRequest(referenceId)?.rejectRequest(referenceId, 4567, "Transaction rejected")
    }

    override fun onTransactionSubmitted(safeAddress: Solidity.Address, transaction: SafeTransaction, chainHash: String, referenceId: Long?) {
        referenceId ?: return
        sessionForRequest(referenceId)?.approveRequest(referenceId, chainHash)
    }

    private fun sessionForRequest(referenceId: Long) =
        sessionRequests.remove(referenceId)?.let {
            sessions[it]
        }

    override fun sessions(): Single<List<BridgeRepository.SessionMeta>> =
        Single.fromCallable {
            sessionStore.list().map { it.toSessionMeta() }
        }

    override fun observeSessions(): Observable<List<BridgeRepository.SessionMeta>> =
        sessionUpdates.switchMapSingle { sessions().onErrorReturnItem(emptyList()) }

    override fun observeActiveSessionIds(): Observable<List<String>> =
        sessionUpdates.map { sessions.keys.sorted() }

    override fun session(sessionId: String): Single<BridgeRepository.SessionMeta> =
        Single.fromCallable {
            sessionStore.load(sessionId)?.toSessionMeta() ?: throw NoSuchElementException()
        }

    private fun WCSessionStore.State.toSessionMeta() =
        BridgeRepository.SessionMeta(
            config.handshakeTopic,
            peerData?.meta?.name,
            peerData?.meta?.description,
            peerData?.meta?.icons,
            sessions.containsKey(config.handshakeTopic),
            approvedAccounts?.map { acc -> acc.asEthereumAddress()!! }
        )

    private fun internalGetOrCreateSession(config: Session.Config) =
        sessions[config.handshakeTopic]?.let { config.handshakeTopic } ?: internalCreateSession(config)

    private fun internalCreateSession(config: Session.Config) =
        sessionBuilder.build(
            config,
            Session.PayloadAdapter.PeerMeta(name = context.getString(R.string.app_name))
        ).let {
            it.addCallback(object : Session.Callback {

                @SuppressLint("CheckResult")
                override fun sendTransaction(
                    id: Long,
                    from: String,
                    to: String,
                    nonce: String,
                    gasPrice: String,
                    gasLimit: String,
                    value: String,
                    data: String
                ) {
                    sessionRequests[id] = config.handshakeTopic
                    Single.fromCallable {
                        val safe = from.asEthereumAddress() ?: throw IllegalArgumentException("Invalid Safe address: $from")
                        val txTo = to.asEthereumAddress() ?: throw IllegalArgumentException("Invalid to address: $to")
                        val txValue = value.hexAsBigIntegerOrNull() ?: throw IllegalArgumentException("Invalid to value: $value")
                        safe to SafeTransaction(
                            Transaction(txTo, value = Wei(txValue), data = data),
                            TransactionExecutionRepository.Operation.CALL
                        )
                    }
                        .flatMap { (safe, tx) -> infoRepository.parseTransactionData(tx).map { txData -> safe to txData } }
                        .subscribeBy(onError = { t ->
                            rejectRequest(id, 42, t.message ?: "Could not handle transaction")
                        }) { (safe, txData) ->
                            showSendTransactionNotification(safe, txData, id)
                        }

                }

                override fun signMessage(id: Long, address: String, message: String) {
                    it.rejectRequest(id, 1123, "The Gnosis Safe doesn't support eth_sign")
                }

                override fun sessionRequest(peer: Session.PayloadAdapter.PeerData) {}

                override fun sessionApproved() {}

                override fun sessionClosed(msg: String?) {
                    sessions.remove(config.handshakeTopic)
                    sessionUpdates.onNext(Unit)
                }
            })
            sessions[config.handshakeTopic] = it
            sessionUpdates.onNext(Unit)
            config.handshakeTopic
        }

    private fun showSendTransactionNotification(safe: Solidity.Address, data: TransactionData, referenceId: Long) {
        val intent = ReviewTransactionActivity.createIntent(context, safe, data, referenceId)
        localNotificationManager.show(
            referenceId.hashCode(),
            context.getString(R.string.sign_transaction_request_title),
            context.getString(R.string.sign_transaction_request_message),
            intent
        )
    }

    override fun createSession(url: String): String =
        internalGetOrCreateSession(Session.Config.fromWCUri(url)).also {
            startBridgeService()
        }


    override fun observeSession(sessionId: String): Observable<BridgeRepository.SessionEvent> =
        sessionUpdates
            .map { sessions.containsKey(sessionId) }
            .scan { old, new -> old != new }.filter { it } // Only trigger updates if activity changes
            .switchMap { createSessionObservable(sessionId) }

    private fun createSessionObservable(sessionId: String): Observable<BridgeRepository.SessionEvent> =
        Observable.create<BridgeRepository.SessionEvent> { emitter ->
            try {
                // Emit current state
                sessionStore.load(sessionId)?.let {
                    val peer = it.peerData
                    emitter.onNext(BridgeRepository.SessionEvent.MetaUpdate(BridgeRepository.SessionMeta(
                        sessionId,
                        peer?.meta?.name,
                        peer?.meta?.description,
                        peer?.meta?.icons,
                        sessions.containsKey(sessionId),
                        it.approvedAccounts?.map { acc -> acc.asEthereumAddress()!! }
                    )))
                }
                val session = sessions[sessionId] ?: run {
                    emitter.onComplete()
                    return@create
                }
                val cb = object : Session.Callback {

                    override fun sendTransaction(
                        id: Long,
                        from: String,
                        to: String,
                        nonce: String,
                        gasPrice: String,
                        gasLimit: String,
                        value: String,
                        data: String
                    ) {
                        emitter.onNext(BridgeRepository.SessionEvent.Transaction(id, from, to, nonce, gasPrice, gasLimit, value, data))
                    }

                    override fun signMessage(id: Long, address: String, message: String) {}

                    override fun sessionClosed(msg: String?) {
                        emitter.onNext(BridgeRepository.SessionEvent.Closed(sessionId))
                        emitter.onComplete()
                    }

                    override fun sessionRequest(peer: Session.PayloadAdapter.PeerData) {
                        emitter.onNext(
                            BridgeRepository.SessionEvent.SessionRequest(
                                BridgeRepository.SessionMeta(
                                    sessionId,
                                    peer.meta?.name,
                                    peer.meta?.description,
                                    peer.meta?.icons,
                                    sessions.containsKey(sessionId),
                                    null
                                )
                            )
                        )
                    }

                    override fun sessionApproved() {
                        sessionMeta(sessionId)?.let { emitter.onNext(it) }
                    }
                }
                emitter.setCancellable {
                    session.removeCallback(cb)
                }
                session.addCallback(cb)

            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
            .subscribeOn(Schedulers.io())

    private fun sessionMeta(sessionId: String) =
        sessions[sessionId]?.let {
            val meta = it.peerMeta()
            BridgeRepository.SessionEvent.MetaUpdate(
                BridgeRepository.SessionMeta(
                    sessionId,
                    meta?.name,
                    meta?.description,
                    meta?.icons,
                    sessions.containsKey(sessionId),
                    it.approvedAccounts()?.map { acc -> acc.asEthereumAddress()!! }
                )
            )
        }

    override fun initSession(sessionId: String): Completable = Completable.fromAction {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not active")
        session.init()
    }
        .subscribeOn(Schedulers.io())

    override fun approveSession(sessionId: String): Completable =
        safeRepository.observeSafes().firstOrError()
            .flatMapCompletable { safes ->
                if (safes.isEmpty()) throw IllegalStateException("No Safe to whitelist")
                Completable.fromAction {
                    val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not active")
                    session.approve(safes.map { it.address.asEthereumAddressChecksumString() }, BuildConfig.BLOCKCHAIN_CHAIN_ID)
                }
            }
            .subscribeOn(Schedulers.io())

    override fun rejectSession(sessionId: String): Completable = Completable.fromAction {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not active")
        session.reject()
    }
        .subscribeOn(Schedulers.io())

    override fun closeSession(sessionId: String): Completable = Completable.fromAction {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not active")
        session.kill()
    }
        .subscribeOn(Schedulers.io())

    override fun approveRequest(requestId: Long, response: Any): Completable = Completable.fromAction {
        val session = sessionForRequest(requestId) ?: throw IllegalArgumentException("Session not found")
        session.approveRequest(requestId, response)
    }
        .subscribeOn(Schedulers.io())

    override fun rejectRequest(requestId: Long, errorCode: Long, errorMsg: String): Completable = Completable.fromAction {
        val session = sessionForRequest(requestId) ?: throw IllegalArgumentException("Session not found")
        session.rejectRequest(requestId, errorCode, errorMsg)
    }
        .subscribeOn(Schedulers.io())

    override fun activateSession(sessionId: String): Completable = Completable.fromAction {
        val session = sessionStore.load(sessionId) ?: throw IllegalArgumentException("Session not found")
        internalGetOrCreateSession(session.config)
        startBridgeService()
    }
        .subscribeOn(Schedulers.io())

    private fun startBridgeService() {
        context.startService(Intent(context, BridgeService::class.java))
    }
}

interface SessionBuilder {
    fun build(config: Session.Config, clientMeta: Session.PayloadAdapter.PeerMeta): Session
}

@Singleton
class WCSessionBuilder @Inject constructor(
    private val sessionStore: WCSessionStore,
    private val sessionPayloadAdapter: Session.PayloadAdapter,
    private val sessionTransportBuilder: Session.Transport.Builder
) : SessionBuilder {
    override fun build(config: Session.Config, clientMeta: Session.PayloadAdapter.PeerMeta): Session = WCSession(
        config,
        sessionPayloadAdapter,
        sessionStore,
        sessionTransportBuilder,
        clientMeta
    )
}


interface Session {
    fun init()
    fun approve(accounts: List<String>, chainId: Long)
    fun reject()
    fun update(accounts: List<String>, chainId: Long)
    fun kill()

    fun peerMeta(): PayloadAdapter.PeerMeta?
    fun approvedAccounts(): List<String>?

    fun approveRequest(id: Long, response: Any)
    fun rejectRequest(id: Long, errorCode: Long, errorMsg: String)

    fun addCallback(cb: Session.Callback)
    fun removeCallback(cb: Session.Callback)

    data class Config(
        val handshakeTopic: String,
        val bridge: String,
        val key: String,
        val protocol: String = "wc",
        val version: Int = 1
    ) {
        companion object {
            fun fromWCUri(uri: String): Config {
                val protocolSeparator = uri.indexOf(':')
                val handshakeTopicSeparator = uri.indexOf('@', startIndex = protocolSeparator)
                val versionSeparator = uri.indexOf('?')
                val protocol = uri.substring(0, protocolSeparator)
                val handshakeTopic = uri.substring(protocolSeparator + 1, handshakeTopicSeparator)
                val version = Integer.valueOf(uri.substring(handshakeTopicSeparator + 1, versionSeparator))
                val params = uri.substring(versionSeparator + 1).split("&").associate {
                    it.split("=").let { param -> param.first() to URLDecoder.decode(param[1], "UTF-8") }
                }
                val bridge = params["bridge"] ?: throw IllegalArgumentException("Missing bridge param in URI")
                val key = params["key"] ?: throw IllegalArgumentException("Missing key param in URI")
                return Config(handshakeTopic, bridge, key, protocol, version)
            }
        }
    }

    interface Callback {

        fun sendTransaction(
            id: Long,
            from: String,
            to: String,
            nonce: String,
            gasPrice: String,
            gasLimit: String,
            value: String,
            data: String
        )

        fun signMessage(id: Long, address: String, message: String)

        fun sessionRequest(peer: PayloadAdapter.PeerData)

        fun sessionApproved()

        fun sessionClosed(msg: String?)
    }

    interface PayloadAdapter {
        fun parse(payload: String, key: String): MethodCall
        fun prepare(data: MethodCall, key: String): String

        sealed class MethodCall(private val internalId: Long) {
            fun id() = internalId

            data class SessionRequest(val id: Long, val peer: PeerData) : MethodCall(id)

            data class SessionUpdate(val id: Long, val params: SessionParams) : MethodCall(id)

            data class ExchangeKey(val id: Long, val nextKey: String, val peer: PeerData) : MethodCall(id)

            data class SendTransaction(
                val id: Long,
                val from: String,
                val to: String,
                val nonce: String,
                val gasPrice: String,
                val gasLimit: String,
                val value: String,
                val data: String
            ) : MethodCall(id)

            data class SignMessage(val id: Long, val address: String, val message: String) : MethodCall(id)

            data class Response(val id: Long, val result: Any?, val error: Error? = null) : MethodCall(id)
        }

        data class PeerData(val id: String, val meta: PeerMeta?)
        data class PeerMeta(
            val url: String? = null,
            val name: String? = null,
            val description: String? = null,
            val icons: List<String>? = null,
            val ssl: Boolean? = null
        )

        data class SessionParams(val approved: Boolean, val chainId: Long?, val accounts: List<String>?, val message: String?)

        data class Error(val code: Long, val message: String)

    }

    interface Transport {

        fun connect(): Boolean

        fun send(message: Message)

        fun status(): Status

        fun close()

        enum class Status {
            CONNECTED,
            DISCONNECTED
        }

        data class Message(
            val topic: String,
            val type: String,
            val payload: String
        )

        interface Builder {
            fun build(
                url: String,
                statusHandler: (Session.Transport.Status) -> Unit,
                messageHandler: (Session.Transport.Message) -> Unit
            ): Transport
        }

    }

    sealed class MethodCallException(val id: Long, val code: Long, message: String) : IllegalArgumentException(message) {
        class InvalidMethod(id: Long, method: String) : MethodCallException(id, 42, "Unknown method: $method")
        class InvalidRequest(id: Long, request: String) : MethodCallException(id, 23, "Invalid request: $request")
        class InvalidAccount(id: Long, account: String) : MethodCallException(id, 3141, "Invalid account request: $account")
    }
}
