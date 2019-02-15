package pm.gnosis.heimdall.data.repositories.impls

import okhttp3.*
import okio.ByteString
import pm.gnosis.heimdall.data.repositories.BridgeReposity
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class WalletConnectBridgeRepository @Inject constructor(
    private val client: OkHttpClient
): BridgeReposity {

    private val sessions: MutableMap<String, Session> = ConcurrentHashMap()

    fun connect(url: String): Session {
        val id = UUID.randomUUID().toString()
        return sessions[id] ?: WCSession(id, url, client).also { sessions[id] = it }
    }

    inner class WCSession(val id: String, session: String, client: OkHttpClient): Session, WebSocketListener() {

        private val socket: WebSocket = client.newWebSocket(Request.Builder().url(session).build(), this)

        override fun send(message: String) {
            socket.send(message)
        }

        override fun close() {
            socket.close(1000, null)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            System.out.println("Socket Open")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            System.out.println("Message Received bytes")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            System.out.println("Message Received string")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            sessions.remove(id)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            sessions.remove(id)
            System.out.println("Socket Closed")
        }
    }
}

interface Session {
    fun send(message: String)
    fun close()
}
