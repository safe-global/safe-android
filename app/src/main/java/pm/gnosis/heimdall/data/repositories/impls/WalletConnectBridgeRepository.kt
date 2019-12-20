package pm.gnosis.heimdall.data.repositories.impls

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.core.os.BuildCompat
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.picasso.Picasso
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.walletconnect.Session
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore
import org.walletconnect.nullOnThrow
import pm.gnosis.ethereum.rpc.models.JsonRpcError
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.preferences.PreferencesWalletConnect
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.BridgeRepository.Companion.MULTI_SEND_RPC
import pm.gnosis.heimdall.data.repositories.BridgeRepository.RejectionReason
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.Operation
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.heimdall.services.BridgeService
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.heimdall.utils.parseToBigInteger
import pm.gnosis.heimdall.utils.parseToBigIntegerOrNull
import pm.gnosis.heimdall.utils.shortChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import retrofit2.http.Body
import retrofit2.http.POST
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

class WalletConnectBridgeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rpcProxyApi: RpcProxyApi,
    private val picasso: Picasso,
    private val infoRepository: TransactionInfoRepository,
    private val localNotificationManager: LocalNotificationManager,
    private val sessionStore: WCSessionStore,
    private val sessionBuilder: SessionBuilder,
    private val prefs: PreferencesWalletConnect,
    executionRepository: TransactionExecutionRepository
) : BridgeRepository, TransactionExecutionRepository.TransactionEventsCallback {

    private val sessionUpdates = BehaviorSubject.createDefault(Unit)
    private val sessions: MutableMap<String, Session> = ConcurrentHashMap()

    private val sessionRequests: MutableMap<Long, String> = ConcurrentHashMap()

    init {
        executionRepository.addTransactionEventsCallback(this)
    }

    override fun onTransactionRejected(referenceId: Long) {
        rejectRequest(referenceId, RejectionReason.Rejected).subscribe()
    }

    override fun onTransactionSubmitted(safeAddress: Solidity.Address, transaction: SafeTransaction, chainHash: String, referenceId: Long?) {
        referenceId ?: return
        sessionForRequest(referenceId)?.approveRequest(referenceId, chainHash)
    }

    override fun init() {
        localNotificationManager.createNotificationChannel(
            CHANNEL_WALLET_CONNECT_REQUESTS,
            context.getString(R.string.channel_description_wallect_connect_requests)
        )
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

    private fun getSafeForSession(sessionId: String) = prefs.safeForSession(sessionId)

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
                override fun onMethodCall(call: Session.MethodCall) {
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
                                        Operation.CALL
                                    )
                                }
                                    .flatMap { (safe, tx) ->
                                        infoRepository.checkRestrictedTransaction(safe, tx)
                                            .flatMap(infoRepository::parseTransactionData)
                                            .map { txData -> safe to txData }
                                    }
                                    .subscribeBy(onError = { t ->
                                        rejectRequest(call.id, RejectionReason.AppError(t)).subscribe()
                                    }) { (safe, txData) ->
                                        showSendTransactionNotification(session.peerMeta(), safe, txData, id, sessionId)
                                    }
                            }
                        is Session.MethodCall.SignMessage ->
                            session.rejectRequest(call.id, RejectionReason.Unsupported("eth_sign"))
                        is Session.MethodCall.Custom ->
                            handleCustomCall(sessionId, session, call)
                    }
                }

                override fun onStatus(status: Session.Status) {
                    when (status) {
                        Session.Status.Connected -> {

                        }
                        Session.Status.Disconnected -> {

                        }
                        Session.Status.Approved -> {

                        }
                        Session.Status.Closed -> {
                            prefs.removeSession(sessionId)
                            sessions.remove(sessionId)
                            sessionUpdates.onNext(Unit)
                        }
                        is Session.Status.Error -> {
                            Timber.e(status.throwable)
                        }
                    }
                }
            })
            sessions[config.handshakeTopic] = session
            sessionUpdates.onNext(Unit)
            config.handshakeTopic
        }

    private fun handleCustomCall(sessionId: String, session: Session, call: Session.MethodCall.Custom) {
        sessionRequests[call.id] = sessionId
        try {
            when (call.method) {
                MULTI_SEND_RPC -> {
                    handleMultiSend(sessionId, session, call)
                }
                else ->
                    call.apply {
                        rpcProxyApi.proxy(RpcProxyApi.ProxiedRequest(method, (params as? List<Any>) ?: emptyList(), id))
                            .subscribeBy(onError = { t -> rejectRequest(call.id, RejectionReason.AppError(t)).subscribe() }) { result ->
                                result.error?.let { error ->
                                    rejectRequest(id, RejectionReason.RPCError(error.code.toLong(), error.message)).subscribe()
                                } ?: run {
                                    approveRequest(id, result.result ?: "").subscribe()
                                }
                            }
                    }
            }
        } catch (e: Exception) {
            rejectRequest(call.id, RejectionReason.AppError(e)).subscribe()
        }
    }

    @SuppressLint("CheckResult")
    private fun handleMultiSend(sessionId: String, session: Session, call: Session.MethodCall.Custom) {
        val safe = session.approvedAccounts()?.firstOrNull()?.asEthereumAddress() ?: throw IllegalArgumentException("No whitelisted Safe!")
        val params = (call.params as? List<Map<String, String>>) ?: emptyList()
        val txs = params.map {
            val txOperation = it["operation"]?.toInt()?.let { op -> Operation.fromInt(op) } ?: Operation.CALL
            val txTo = it["to"]?.asEthereumAddress() ?: throw IllegalArgumentException("Invalid to address: ${it["to"]}")
            val txValue =
                it["value"]?.run {
                    parseToBigIntegerOrNull() ?: throw IllegalArgumentException("Invalid to value: $this")
                } ?: BigInteger.ZERO
            val txData = it["data"]?.apply { hexStringToByteArray() }?.addHexPrefix() // Check that it is valid hex data
            SafeTransaction(Transaction(txTo, value = Wei(txValue), data = txData), txOperation)
        }
        val peerMeta = session.peerMeta()
        Observable.fromIterable(txs).flatMapSingle {
            infoRepository.checkRestrictedTransaction(safe, it)
        }
            .toList()
            .subscribeBy(
                onSuccess = {
                    showSendTransactionNotification(peerMeta, safe, TransactionData.MultiSend(txs, MULTI_SEND_LIB), call.id, sessionId)
                },
                onError = { t -> rejectRequest(call.id, RejectionReason.AppError(t, MULTI_SEND_RPC)).subscribe() })
    }

    private fun showSendTransactionNotification(
        peerMeta: Session.PeerMeta?,
        safe: Solidity.Address,
        data: TransactionData,
        referenceId: Long,
        sessionId: String
    ) {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = ReviewTransactionActivity.createIntent(context, safe, data, referenceId, sessionId)
        // Pre Android Q we will directly show the review activity if the phone is unlocked, else we show a notification
        // TODO: Adjust check when Q is released
        if (BuildCompat.isAtLeastQ() || Build.VERSION.SDK_INT > Build.VERSION_CODES.P || keyguard.isKeyguardLocked) {
            val icon = peerMeta?.icons?.firstOrNull()?.let { nullOnThrow { picasso.load(it).get() } }
            val notification = localNotificationManager.builder(
                peerMeta?.name ?: context.getString(R.string.unknown_dapp),
                context.getString(R.string.notification_new_transaction_request),
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT),
                CHANNEL_WALLET_CONNECT_REQUESTS
            )
                .setSubText(safe.shortChecksumString())
                .setLargeIcon(icon)
                .build()
            localNotificationManager.show(
                referenceId.hashCode(),
                notification
            )
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun createSession(url: String, safe: Solidity.Address): String =
        Session.Config.fromWCUri(url).let { config ->
            val sessionId = config.handshakeTopic
            prefs.saveSession(sessionId, safe)
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

                    override fun onMethodCall(call: Session.MethodCall) {
                        when (call) {
                            is Session.MethodCall.SendTransaction ->
                                call.apply {
                                    emitter.onNext(BridgeRepository.SessionEvent.Transaction(id, from, to, nonce, gasPrice, gasLimit, value, data))
                                }
                        }
                    }

                    override fun onStatus(status: Session.Status) {
                        when (status) {
                            Session.Status.Connected -> {

                            }
                            Session.Status.Disconnected -> {

                            }
                            Session.Status.Approved -> {
                                sessionMeta(sessionId)?.let { emitter.onNext(it) }
                            }
                            Session.Status.Closed -> {
                                emitter.onNext(BridgeRepository.SessionEvent.Closed(sessionId))
                                emitter.onComplete()
                            }
                            is Session.Status.Error -> {
                                Timber.e(status.throwable)
                            }
                        }
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

    private fun Session.rejectRequest(requestId: Long, reason: RejectionReason) =
        rejectRequest(requestId, reason.code, reason.message)

    override fun rejectRequest(requestId: Long, reason: RejectionReason): Completable = Completable.fromAction {
        val session = sessionForRequest(requestId) ?: throw IllegalArgumentException("Session not found")
        session.rejectRequest(requestId, reason)
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
            !prefs.introDone
        }
            .subscribeOn(Schedulers.io())

    override fun markIntroDone(): Completable =
        Completable.fromAction {
            prefs.introDone = true
        }
            .subscribeOn(Schedulers.io())

    companion object {
        private const val CHANNEL_WALLET_CONNECT_REQUESTS = "channel_wallet_connect_requests"
        private val MULTI_SEND_LIB = BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!
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
