package pm.gnosis.heimdall.ui.two_factor.keycard

import android.nfc.NfcAdapter
import im.status.keycard.android.NFCCardManager
import im.status.keycard.io.CardChannel
import im.status.keycard.io.CardListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

// TODO inject this for better testing
class WrappedManager : CardListener {
    private val listeners: MutableList<CardListener> = CopyOnWriteArrayList()

    private val internalManager = NFCCardManager().apply {
        setCardListener(this@WrappedManager)
        start()
    }

    fun callback(): NfcAdapter.ReaderCallback = internalManager

    override fun onConnected(channel: CardChannel?) {
        listeners.forEach { it.onConnected(channel) }
    }

    override fun onDisconnected() {
        listeners.forEach { it.onDisconnected() }
    }

    fun addListener(listener: CardListener) =
        listeners.add(listener)

    fun removeListener(listener: CardListener) =
        listeners.remove(listener)

    suspend fun performOnChannel(action: (channel: CardChannel) -> Unit) {
        suspendCancellableCoroutine<Unit> { cont ->
            val listener = object : CardListener {
                override fun onConnected(channel: CardChannel?) {
                    channel ?: return
                    try {
                        action(channel)
                        cont.resume(Unit) {}
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }

                override fun onDisconnected() {
                    Timber.d("KeyCard disconnected.")
                }

            }
            cont.invokeOnCancellation {
                removeListener(listener)
            }
            addListener(listener)
        }
    }
}
