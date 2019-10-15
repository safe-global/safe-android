package pm.gnosis.heimdall.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.disposables.CompositeDisposable
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.data.remote.models.push.PushMessage
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import timber.log.Timber
import javax.inject.Inject

class HeimdallFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var pushRepository: PushServiceRepository

    private val disposables = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        HeimdallApplication[this].inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // No data received
        if (message.data.isEmpty()) return

        try {
            pushRepository.handlePushMessage(PushMessage.fromMap(message.data))
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        pushRepository.syncAuthentication()
    }
}
