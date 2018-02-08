package pm.gnosis.heimdall.data.remote.impls

import com.google.firebase.messaging.FirebaseMessaging
import pm.gnosis.heimdall.data.remote.MessageQueueRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMessageQueueRepository @Inject constructor():
        MessageQueueRepository {

    private val firebase = FirebaseMessaging.getInstance()

    override fun unsubscribe(topic: String) =
            firebase.unsubscribeFromTopic(topic)

    override fun subscribe(topic: String) =
            firebase.subscribeToTopic(topic)

}
