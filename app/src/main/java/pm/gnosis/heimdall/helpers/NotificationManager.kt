package pm.gnosis.heimdall.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.services.HeimdallFirebaseService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLocalNotificationManager @Inject constructor(
        private @ApplicationContext val context: Context
) : LocalNotificationManager {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }
        val name = context.getString(R.string.channel_signing_request_name)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = context.getString(R.string.channel_signing_request_description)

        channel.enableLights(true)
        channel.lightColor = LIGHT_COLOR

        channel.enableVibration(true)
        channel.vibrationPattern = VIBRATE_PATTERN

        notificationManager?.createNotificationChannel(channel)
    }

    override fun hide(id: Int) {
        notificationManager?.cancel(id)
    }

    override fun show(id: Int, title: String, message: String, intent: Intent) {
        val builder =  NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_gno)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setVibrate(VIBRATE_PATTERN)
                .setLights(LIGHT_COLOR, 100, 100)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))

        notificationManager?.notify(id, builder.build())
    }

    companion object {
        private val VIBRATE_PATTERN = longArrayOf(0, 100, 50, 100)
        private const val LIGHT_COLOR = Color.MAGENTA
        private const val CHANNEL_ID = "channel_signing_requests"
    }
}

interface LocalNotificationManager {
    fun hide(id: Int)

    fun show(id: Int, title: String, message: String, intent: Intent)
}
