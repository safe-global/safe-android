package pm.gnosis.heimdall.data.repositories.impls

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.repositories.BridgeReposity
import pm.gnosis.heimdall.data.repositories.impls.wc.*
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class WalletConnectBridgeRepository @Inject constructor(
    private val client: OkHttpClient,
    private val moshi: Moshi,
    private val sessionStore: WCSessionStore
) : BridgeReposity {
    private val sessions: MutableMap<String, Session> = ConcurrentHashMap()

    override fun sessions(): Single<List<BridgeReposity.SessionMeta>> =
        Single.fromCallable {
            sessionStore.list().map {
                BridgeReposity.SessionMeta(
                    it.config.handshakeTopic,
                    it.peerData?.meta?.name,
                    it.peerData?.meta?.description,
                    it.peerData?.meta?.icons,
                    sessions.containsKey(it.config.handshakeTopic),
                    it.approvedAccounts?.map { acc -> acc.asEthereumAddress()!! }
                )
            }
        }

    override fun createSession(url: String): String {
        val config = Session.Config.fromWCUri(url)
        return (sessions[config.handshakeTopic]?.let { config.handshakeTopic } ?: WCSession(
            config,
            MoshiPayloadAdapter(moshi),
            sessionStore,
            OkHttpTransport.Builder(client, moshi),
            Session.PayloadAdapter.PeerMeta(name = "Gnosis Safe")
        ).let {
            it.addCallback(object : Session.Callback {
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
                    // TODO show notification or screen
                }

                override fun sessionRequest(peer: Session.PayloadAdapter.PeerData) {}

                override fun sessionClosed(msg: String?) {
                    sessions.remove(config.handshakeTopic)
                }
            })
            sessions[config.handshakeTopic] = it
            config.handshakeTopic
        })
    }

    override fun observeSession(sessionId: String): Observable<Any> = Observable.create<Any> { emitter ->
        try {
            val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
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
                    emitter.onNext(BridgeReposity.SessionEvent.Transaction(id, from, to, nonce, gasPrice, gasLimit, value, data))
                }

                override fun sessionClosed(msg: String?) {
                    emitter.onNext(BridgeReposity.SessionEvent.Closed(sessionId))
                }

                override fun sessionRequest(peer: Session.PayloadAdapter.PeerData) {
                    emitter.onNext(
                        BridgeReposity.SessionEvent.SessionRequest(
                            BridgeReposity.SessionMeta(
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

    override fun initSession(sessionId: String): Completable = Completable.fromAction {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        session.init()
    }
        .subscribeOn(Schedulers.io())

    override fun approveSession(sessionId: String, safe: Solidity.Address): Completable = Completable.fromAction {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        session.approve(listOf(safe.asEthereumAddressChecksumString()), BuildConfig.BLOCKCHAIN_CHAIN_ID)
    }
        .subscribeOn(Schedulers.io())

    override fun closeSession(sessionId: String): Completable = Completable.fromAction {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        session.kill()
    }
        .subscribeOn(Schedulers.io())

    override fun approveRequest(sessionId: String, requestId: Long, response: Any): Completable = Completable.fromAction {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        session.approveRequest(requestId, response)
    }
        .subscribeOn(Schedulers.io())

    override fun rejectRequest(sessionId: String, requestId: Long, errorCode: Long, errorMsg: String): Completable = Completable.fromAction {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        session.rejectRequest(requestId, errorCode, errorMsg)
    }
        .subscribeOn(Schedulers.io())
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

        fun sessionRequest(peer: PayloadAdapter.PeerData)

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

        @JsonClass(generateAdapter = true) // TODO decouple somehow
        data class Message(val topic: String, val type: String, val payload: String)

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
        class InvalidAccount(id: Long, account: String) : MethodCallException(id, 3141, "Invalid account request: $account")
    }
}
