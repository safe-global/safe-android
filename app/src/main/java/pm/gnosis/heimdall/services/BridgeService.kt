package pm.gnosis.heimdall.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.helpers.AppPreferencesManager
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.heimdall.ui.safe.main.SafeMainViewModel
import pm.gnosis.heimdall.ui.walletconnect.WalletConnectSessionsActivity
import pm.gnosis.model.Solidity
import timber.log.Timber
import javax.inject.Inject


class BridgeService : Service() {

    @Inject
    lateinit var bridgeRepository: BridgeRepository

    @Inject
    lateinit var localNotificationManager: LocalNotificationManager

    @Inject
    lateinit var preferencesManager: AppPreferencesManager

    private val disposables = CompositeDisposable()

    private var enabled = false

    override fun onCreate() {
        super.onCreate()
        HeimdallApplication[this].inject(this)

        localNotificationManager.createNotificationChannel(NOTIFICATION_CHANNEL, "Updates of WalletConnect connections", 0)
    }

    private fun checkEnabled() {
        if (!enabled) {
            enable()
            disposables += bridgeRepository.observeActiveSessions()
                .subscribeBy(onNext = ::updateNotification, onError = Timber::e)
        }
    }

    private fun updateNotification(sessionIds: List<String>) {
        if (sessionIds.isEmpty()) {
            disable()
        } else {
            localNotificationManager.show(NOTIFICATION_ID, notification(sessionIds.size))
        }
    }

    private fun enable() {
        enabled = true
        // Start foreground service.
        startForeground(NOTIFICATION_ID, notification(0))
    }

    private fun notification(count: Int): Notification {
        val intent = WalletConnectSessionsActivity.createIntent(this, null)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val message = if (count == 0) "Loading WalletConnect status ..." else "$count active WalletConnect sessions"
        return localNotificationManager.builder(
            "Connected to WalletConnect",
            message,
            pendingIntent,
            NOTIFICATION_CHANNEL,
            NotificationCompat.CATEGORY_SERVICE,
            NotificationCompat.PRIORITY_MIN
        ).build()
    }

    private fun disable() {
        disposables.clear()
        enabled = false
        stopForeground(true)
        localNotificationManager.hide(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkEnabled()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder(this)

    class LocalBinder(val service: BridgeService) : Binder()

    companion object {
        private const val NOTIFICATION_ID = 3141
        private const val NOTIFICATION_CHANNEL = "wallet_connect_notification_channel"
    }

}
