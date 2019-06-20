package pm.gnosis.heimdall.data.repositories.impls

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.walletconnect.Session
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore
import pm.gnosis.ethereum.rpc.models.JsonRpcError
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.AppPreferencesManager
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.heimdall.services.BridgeService
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigIntegerOrNull
import retrofit2.http.Body
import retrofit2.http.POST
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

class WalletConnectBridgeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rpcProxyApi: RpcProxyApi,
    private val infoRepository: TransactionInfoRepository,
    private val localNotificationManager: LocalNotificationManager,
    private val sessionStore: WCSessionStore,
    private val sessionBuilder: SessionBuilder,
    appPreferencesManager: AppPreferencesManager,
    executionRepository: TransactionExecutionRepository
) : BridgeRepository, TransactionExecutionRepository.TransactionEventsCallback {

    private val bridgePreferences = appPreferencesManager.get(PREFERENCES_BRIDGE)
    private val sessionsPreferences = appPreferencesManager.get(PREFERENCES_SESSIONS)
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

    override fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network?) {
                        super.onAvailable(network)
                        activateAllSessions()
                    }
                })
        }
    }

    private fun sessionForRequest(referenceId: Long) =
        sessionRequests.remove(referenceId)?.let {
            sessions[it]
        }

    private fun activateAllSessions() =
        Single.fromCallable {
            sessionStore.list()
                .map { session -> internalGetOrCreateSession(session.config) }
                .map { sessions[it]?.init() }
            startBridgeService()
        }
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = Timber::e)

    override fun sessions(safe: Solidity.Address?): Single<List<BridgeRepository.SessionMeta>> =
        Single.fromCallable {
            sessionStore.list()
                .filter { safe == null || getSafeForSession(it.config.handshakeTopic) == safe }
                .map { it.toSessionMeta() }
        }

    private fun getSafeForSession(sessionId: String) =
        sessionsPreferences.getString(KEY_PREFIX_SESSION_SAFE + sessionId, null)?.asEthereumAddress()

    override fun observeSessions(safe: Solidity.Address?): Observable<List<BridgeRepository.SessionMeta>> =
        sessionUpdates.switchMapSingle { sessions(safe).onErrorReturnItem(emptyList()) }

    override fun observeActiveSessionInfo(): Observable<List<SessionIdAndSafe>> =
        sessionUpdates.map {
            sessions.keys.mapNotNull { sessionId ->
                getSafeForSession(sessionId)?.let { sessionId to it }
            }.sortedBy { it.first }
        }

    override fun session(sessionId: String): Single<BridgeRepository.SessionMeta> =
        Single.fromCallable {
            sessionStore.load(sessionId)?.toSessionMeta() ?: throw NoSuchElementException()
        }

    private fun WCSessionStore.State.toSessionMeta() =
        BridgeRepository.SessionMeta(
            config.handshakeTopic,
            peerData?.meta?.name,
            peerData?.meta?.description,
            peerData?.meta?.url,
            peerData?.meta?.icons,
            sessions.containsKey(config.handshakeTopic),
            approvedAccounts?.map { acc -> acc.asEthereumAddress()!! }
        )

    private fun internalGetOrCreateSession(config: Session.Config) =
        sessions[config.handshakeTopic]?.let { config.handshakeTopic } ?: internalCreateSession(config)

    private fun internalCreateSession(config: Session.Config) =
        sessionBuilder.build(
            config,
            Session.PeerMeta(name = context.getString(R.string.app_name))
        ).let { session ->
            val sessionId = config.handshakeTopic
            session.addCallback(object : Session.Callback {

                @SuppressLint("CheckResult")
                override fun handleMethodCall(call: Session.MethodCall) {
                    when (call) {
                        is Session.MethodCall.SessionRequest -> {
                            getSafeForSession(sessionId)?.let {
                                session.approve(listOf(it.asEthereumAddressString()), BuildConfig.BLOCKCHAIN_CHAIN_ID)
                            } ?: run {
                                session.reject()
                            }
                        }
                        is Session.MethodCall.SendTransaction ->
                            call.apply {
                                sessionRequests[id] = sessionId
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
                                        rejectRequest(id, 42, t.message ?: "Could not handle transaction").subscribe()
                                    }) { (safe, txData) ->
                                        showSendTransactionNotification(safe, txData, id)
                                    }
                            }
                        is Session.MethodCall.SignMessage ->
                            call.apply {
                                session.rejectRequest(id, 1123, "The Gnosis Safe doesn't support eth_sign")
                            }
                        is Session.MethodCall.Custom ->
                            call.apply {
                                sessionRequests[id] = sessionId
                                try {
                                    rpcProxyApi.proxy(RpcProxyApi.ProxiedRequest(method, (params as? List<Any>) ?: emptyList(), id))
                                        .subscribeBy(onError = { t ->
                                            rejectRequest(id, 42, t.message ?: "Could not handle custom call")
                                        }) { result ->
                                            result.error?.let { error ->
                                                rejectRequest(id, error.code.toLong(), error.message).subscribe()
                                            } ?: run {
                                                approveRequest(id, result.result ?: "").subscribe()
                                            }
                                        }
                                } catch (e: Exception) {
                                    Timber.e(e)
                                    rejectRequest(id, 42, "Could not handle custom call: $e").subscribe()
                                }
                            }
                    }
                }

                override fun sessionApproved() {}

                override fun sessionClosed() {
                    sessionsPreferences.edit { remove(KEY_PREFIX_SESSION_SAFE + sessionId) }
                    sessions.remove(sessionId)
                    sessionUpdates.onNext(Unit)
                }
            })
            sessions[config.handshakeTopic] = session
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

    override fun createSession(url: String, safe: Solidity.Address): String =
        Session.Config.fromWCUri(url).let { config ->
            val sessionId = config.handshakeTopic
            sessionsPreferences.edit { putString(KEY_PREFIX_SESSION_SAFE + sessionId, safe.asEthereumAddressString()) }
            internalGetOrCreateSession(config)
            startBridgeService()
            sessionId
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
                        peer?.meta?.url,
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

                    override fun handleMethodCall(call: Session.MethodCall) {
                        when (call) {
                            is Session.MethodCall.SendTransaction ->
                                call.apply {
                                    emitter.onNext(BridgeRepository.SessionEvent.Transaction(id, from, to, nonce, gasPrice, gasLimit, value, data))
                                }
                        }
                    }

                    override fun sessionClosed() {
                        emitter.onNext(BridgeRepository.SessionEvent.Closed(sessionId))
                        emitter.onComplete()
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
                    meta?.url,
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

    override fun shouldShowIntro(): Single<Boolean> =
        Single.fromCallable {
            !bridgePreferences.getBoolean(KEY_INTRO_DONE, false)
        }
            .subscribeOn(Schedulers.io())

    override fun markIntroDone(): Completable =
        Completable.fromAction {
            bridgePreferences.edit { putBoolean(KEY_INTRO_DONE, true) }
        }
            .subscribeOn(Schedulers.io())

    companion object {
        private const val KEY_PREFIX_SESSION_SAFE = "session_safe_"
        private const val PREFERENCES_SESSIONS = "preferences.wallet_connect_bridge_repository.sessions"
        private const val PREFERENCES_BRIDGE = "preferences.wallet_connect_bridge_repository.bridge"
        private const val KEY_INTRO_DONE = "wallet_connect_bridge_repository.boolean.intro_done"
    }
}

interface SessionBuilder {
    fun build(config: Session.Config, clientMeta: Session.PeerMeta): Session
}

@Singleton
class WCSessionBuilder @Inject constructor(
    private val sessionStore: WCSessionStore,
    private val sessionPayloadAdapter: Session.PayloadAdapter,
    private val sessionTransportBuilder: Session.Transport.Builder
) : SessionBuilder {
    override fun build(config: Session.Config, clientMeta: Session.PeerMeta): Session = WCSession(
        config,
        sessionPayloadAdapter,
        sessionStore,
        sessionTransportBuilder,
        clientMeta
    )
}

interface RpcProxyApi {
    @POST(".")
    fun proxy(@Body jsonRpcRequest: ProxiedRequest): Single<ProxiedResult>


    @JsonClass(generateAdapter = true)
    data class ProxiedRequest(
        @Json(name = "method") val method: String,
        @Json(name = "params") val params: List<Any>,
        @Json(name = "id") val id: Long = 1,
        @Json(name = "jsonrpc") val jsonRpc: String = "2.0"
    )


    @JsonClass(generateAdapter = true)
    data class ProxiedResult(
        @Json(name = "id") val id: Long,
        @Json(name = "jsonrpc") val jsonRpc: String,
        @Json(name = "error") val error: JsonRpcError? = null,
        @Json(name = "result") val result: Any?
    )
}
