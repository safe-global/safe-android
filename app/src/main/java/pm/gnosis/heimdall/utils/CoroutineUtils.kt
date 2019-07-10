package pm.gnosis.heimdall.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

@ExperimentalCoroutinesApi
fun <E> CoroutineScope.skipInitial(channel: ReceiveChannel<E>) = produce {
    channel.receive()
    for (e in channel) send(e)
}