package io.gnosis.safe.notifications.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.notifications.models.PushNotification
import io.gnosis.safe.workers.WorkRepository
import io.intercom.android.sdk.push.IntercomPushClient
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

class HeimdallFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepo: NotificationRepository

    @Inject
    lateinit var workRepository: WorkRepository

    @Inject
    lateinit var intercomPushClient: IntercomPushClient

    override fun onCreate() {
        super.onCreate()
        HeimdallApplication[this].inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val message = remoteMessage.data
            // No data received
        if (message.isEmpty()) return

        runBlocking {
            try {
                if (intercomPushClient.isIntercomPush(message)) {
                    //FIXME: workaround to check for unhandled intercom pushes
                    notificationRepo.intercomPushReceived = true
                    intercomPushClient.handlePush(application, message)
                } else {
                    notificationRepo.handlePushNotification(PushNotification.fromMap(message))
                }
            } catch (e: IllegalArgumentException) {
                Timber.e(e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("Firebase token: $token")
        workRepository.registerForPushNotifications(token)
        intercomPushClient.sendTokenToIntercom(application, token)
    }
}
