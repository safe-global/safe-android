package io.gnosis.safe.notifications

import com.google.firebase.messaging.RemoteMessage
import io.gnosis.safe.notifications.models.Notification

class NotificationRepository(
    private val notificationManager: NotificationManager
) {

    fun handleNotification(notification: Notification) {

    }

    // TODO: remove when notification service available (use method above)
    fun handleNotification(message: RemoteMessage) {
        val notification = notificationManager.builder(
            "Incomming transfer",
            message.notification.toString()
        ).build()
        notificationManager.show(
            0, notification
        )
    }

    fun register() {

    }

    fun unregister() {

    }
}
