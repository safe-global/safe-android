package pm.gnosis.heimdall.data.repositories.impls.wc

import pm.gnosis.heimdall.data.repositories.impls.Session
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.toHexString
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class WCSession(
    private val config: Session.Config,
    private val payloadAdapter: Session.PayloadAdapter,
    transportBuilder: Session.Transport.Builder
) : Session {

    private val keyLock = Any()
    private var nextKey: String? = null
    private var currentKey: String = config.key

    private val encryptionKey: String
        get() = currentKey // TODO: check with pedro@walletconnect

    private val decryptionKey: String
        get() = currentKey // TODO: check with pedro@walletconnect

    private var approvedAccounts: List<String>? = null
    private var handshakeId: Long? = null
    private var peerId: String? = null
    private var peerMeta: Session.PayloadAdapter.PeerMeta? = null
    private val transport = transportBuilder.build(config.bridge, ::handleStatus, ::handleMessage)
    private val requests: MutableMap<Long, (Session.PayloadAdapter.MethodCall.Response) -> Unit> = ConcurrentHashMap()
    private var sessionCallbacks: MutableSet<Session.Callback> = Collections.newSetFromMap(ConcurrentHashMap<Session.Callback, Boolean>())
    private val queue: Queue<QueuedMethod> = ConcurrentLinkedQueue()

    override fun addCallback(cb: Session.Callback) {
        sessionCallbacks.add(cb)
    }

    override fun removeCallback(cb: Session.Callback) {
        sessionCallbacks.remove(cb)
    }

    override fun init() {
        if (transport.connect()) {
            // Register for all messages for this client
            transport.send(
                Session.Transport.Message(
                    config.handshakeTopic, "sub", ""
                )
            )
        }
    }

    override fun approve(accounts: List<String>, chainId: Long) {
        val handshakeId = handshakeId ?: return
        approvedAccounts = accounts
        // We should not use classes in the Response, since this will not work with proguard
        val params = Session.PayloadAdapter.SessionParams(true, chainId, accounts, null).intoMap()
        send(Session.PayloadAdapter.MethodCall.Response(handshakeId, params))
    }

    override fun update(accounts: List<String>, chainId: Long) {
        val params = Session.PayloadAdapter.SessionParams(true, chainId, accounts, null)
        send(Session.PayloadAdapter.MethodCall.SessionUpdate(createCallId(), params))
    }

    override fun reject() {
        val handshakeId = handshakeId ?: return
        // We should not use classes in the Response, since this will not work with proguard
        val params = Session.PayloadAdapter.SessionParams(false, null, null, null).intoMap()
        send(Session.PayloadAdapter.MethodCall.Response(handshakeId, params))
    }

    override fun approveRequest(id: Long, response: Any) {
        send(Session.PayloadAdapter.MethodCall.Response(id, response))
    }

    override fun rejectRequest(id: Long, errorCode: Long, errorMsg: String) {
        send(Session.PayloadAdapter.MethodCall.Response(id, result = null, error = Session.PayloadAdapter.Error(errorCode, errorMsg)))
    }

    private fun handleStatus(status: Session.Transport.Status) {
        System.out.println("Status $status")
        when (status) {
            Session.Transport.Status.CONNECTED ->
                // Register for all messages for this client
                transport.send(
                    Session.Transport.Message(
                        config.clientData.id, "sub", ""
                    )
                )
            Session.Transport.Status.DISCONNECTED -> {
            } // noop
        }
    }

    private fun handleMessage(message: Session.Transport.Message) {
        if (message.type != "pub") return
        val data: Session.PayloadAdapter.MethodCall
        synchronized(keyLock) {
            try {
                data = payloadAdapter.parse(message.payload, decryptionKey)
            } catch (e: Exception) {
                handlePayloadError(e)
                return
            }
        }
        System.out.println("Data $data")
        when (data) {
            is Session.PayloadAdapter.MethodCall.SessionRequest -> {
                handshakeId = data.id
                peerId = data.peer.id
                peerMeta = data.peer.meta
                exchangeKey()
                // TODO: should we catch here or assume the implementation of the cb is ok
                sessionCallbacks.forEach { nullOnThrow { it.sessionRequest(data.peer) } }
            }
            is Session.PayloadAdapter.MethodCall.SessionUpdate -> {
                if (!data.params.approved) {
                    internalClose()
                    sessionCallbacks.forEach { nullOnThrow { it.sessionClosed(data.params.message) } }
                }
            }
            is Session.PayloadAdapter.MethodCall.ExchangeKey -> {
                peerId = data.peer.id
                peerMeta = data.peer.meta
                send(Session.PayloadAdapter.MethodCall.Response(data.id, true))
                swapKeys(data.nextKey)
                // TODO: expose peer meta update
            }
            is Session.PayloadAdapter.MethodCall.SendTransaction -> {
                System.out.println(approvedAccounts)
                approvedAccounts?.find { it.toLowerCase() == data.from.toLowerCase() } ?: run {
                    // TODO: Add proper error
                    handlePayloadError(Session.PayloadAdapter.InvalidMethodException(data.id, "wrong from"))
                    return
                }
                sessionCallbacks.forEach {
                    nullOnThrow { it.sendTransaction(data.id, data.from, data.to, data.nonce, data.gasPrice, data.gasLimit, data.value, data.data) }
                }
            }
            is Session.PayloadAdapter.MethodCall.Response -> {
                val callback = requests[data.id] ?: return
                System.out.println("Trigger callback")
                callback(data)
            }
        }
    }

    private fun handlePayloadError(e: Exception) {
        System.out.println("Payload error $e")
        (e as? Session.PayloadAdapter.InvalidMethodException)?.let {
            rejectRequest(it.id, 42, it.message ?: "Unknown error")
        }
    }

    private fun generateKey(length: Int = 256) = ByteArray(length / 8).also { SecureRandom().nextBytes(it) }.toHexString()

    private fun exchangeKey() {
        val nextKey = generateKey()
        System.out.println("Key $nextKey")
        synchronized(keyLock) {
            this.nextKey = nextKey
            send(
                Session.PayloadAdapter.MethodCall.ExchangeKey(
                    createCallId(),
                    nextKey,
                    config.clientData
                ),
                forceSend = true // This is an exchange key ... we should force it
            ) {
                if (it.result as? Boolean == true) {
                    System.out.println("Swap Keys")
                    swapKeys()
                } else {
                    this.nextKey = null
                    drainQueue()
                }
            }
        }
    }

    private fun swapKeys(newKey: String? = nextKey) {
        synchronized(keyLock) {
            newKey?.let {
                this.currentKey = it
                // We always reset the nextKey
                nextKey = null
            }
        }
        drainQueue()
    }

    private fun drainQueue() {
        var method = queue.poll()
        while (method != null) {
            // We could not send it ... bail
            if (!send(method.call, method.topic, false, method.callback)) return
            method = queue.poll()
        }
    }

    // Returns true if method call was handed over to transport
    private fun send(
        msg: Session.PayloadAdapter.MethodCall,
        topic: String? = peerId,
        forceSend: Boolean = false,
        callback: ((Session.PayloadAdapter.MethodCall.Response) -> Unit)? = null
    ): Boolean {
        topic ?: return false // TODO should we throw?
        // Check if key exchange is in progress
        if (!forceSend && nextKey != null) {
            queue.offer(QueuedMethod(topic, msg, callback))
            return false
        }

        val payload: String
        synchronized(keyLock) {
            payload = payloadAdapter.prepare(msg, encryptionKey)
        }
        callback?.let {
            requests[msg.id()] = callback
        }
        System.out.println("Send Request $msg")
        transport.send(Session.Transport.Message(topic, "pub", payload))
        return true
    }

    private fun createCallId() = System.currentTimeMillis()

    private fun internalClose() {
        transport.close()
    }

    override fun close() {
        // TODO: signal peer
        internalClose()
    }

    private data class QueuedMethod(
        val topic: String,
        val call: Session.PayloadAdapter.MethodCall,
        val callback: ((Session.PayloadAdapter.MethodCall.Response) -> Unit)?
    )
}
