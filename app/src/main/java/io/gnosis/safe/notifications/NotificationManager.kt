package io.gnosis.safe.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import io.gnosis.safe.R

class NotificationManager(
    private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        createNotificationChannel(CHANNEL_ID)
    }

    private fun createNotificationChannel(channelId: String, importance: Int = NotificationManager.IMPORTANCE_HIGH) {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }
        val name = context.getString(R.string.channel_tx_notifications_name)
        val channel = NotificationChannel(channelId, name, importance)
        channel.description = context.getString(R.string.channel_tx_notifications_description)

        channel.enableLights(true)
        channel.lightColor = LIGHT_COLOR

        channel.enableVibration(true)
        channel.vibrationPattern = VIBRATE_PATTERN

        notificationManager?.createNotificationChannel(channel)
    }

    fun builder(
        title: String,
        message: String,
        channelId: String = CHANNEL_ID,
        intent: PendingIntent? = null,
        category: String? = null,
        priority: Int = NotificationCompat.PRIORITY_HIGH
    ) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.img_app)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setVibrate(VIBRATE_PATTERN)
            .setLights(LIGHT_COLOR, 100, 100)
            .setDefaults(Notification.DEFAULT_ALL)
            .setCategory(category)
            .setPriority(priority)
            .setContentIntent(intent)!!

    fun show(id: Int, title: String, message: String, channelId: String?, intent: PendingIntent?) {
        val builder = builder(title, message, channelId ?: CHANNEL_ID, intent)
        show(id, builder.build())
    }

    fun show(id: Int, notification: Notification) {
        notificationManager?.notify(id, notification)
    }

    fun hide(id: Int) {
        notificationManager?.cancel(id)
    }

    companion object {
        private val VIBRATE_PATTERN = longArrayOf(0, 100, 50, 100)
        private const val LIGHT_COLOR = Color.MAGENTA
        private const val CHANNEL_ID = "channel_tx_notifications"
    }
}
