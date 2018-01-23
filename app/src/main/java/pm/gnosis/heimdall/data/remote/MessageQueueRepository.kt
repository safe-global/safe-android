package pm.gnosis.heimdall.data.remote

interface MessageQueueRepository {
    fun subscribe(topic: String)
    fun unsubscribe(topic: String)
}
