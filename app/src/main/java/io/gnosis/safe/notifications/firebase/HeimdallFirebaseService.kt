package io.gnosis.safe.notifications.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.notifications.NotificationRepository
import timber.log.Timber
import javax.inject.Inject

class HeimdallFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepo: NotificationRepository


    override fun onCreate() {
        super.onCreate()
        HeimdallApplication[this].inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // No data received
        if (message.data.isEmpty()) return

        try {
            notificationRepo.handleNotification(message)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("Firebase token: $token")
    }
}
