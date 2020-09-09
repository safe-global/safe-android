package io.gnosis.safe.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.R
import io.gnosis.safe.notifications.models.PushNotification
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import io.gnosis.safe.utils.formatForTxList
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit

class NotificationManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val balanceFormatter: BalanceFormatter
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel(CHANNEL_ID)
    }

    private var latestNotificationId: Int
        get() = preferencesManager.prefs.getInt(LATEST_NOTIFICATION_ID, -1)
        set(value) {
            preferencesManager.prefs.edit {
                putInt(LATEST_NOTIFICATION_ID, value)
            }
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

        notificationManager.createNotificationChannel(channel)
    }

    fun builder(
        safe: Safe,
        pushNotification: PushNotification
    ): NotificationCompat.Builder {

        var title = ""
        var text = ""
        var intent: PendingIntent? = null

        val safeName =
            if (safe.localName.isNullOrBlank())
                context.getString(R.string.push_safe_name, safe.address.formatForTxList())
            else
                safe.localName

        when (pushNotification) {
            is PushNotification.ExecutedTransaction -> {
                if (pushNotification.failed) {
                    title = context.getString(R.string.push_title_failed)
                    text = context.getString(R.string.push_text_failed, safeName)
                } else {
                    title = context.getString(R.string.push_title_executed)
                    text = context.getString(R.string.push_text_executed, safeName)
                }
                intent = txDetailsIntent(safe, pushNotification.safeTxHash)
            }
            is PushNotification.IncomingToken -> {
                if (pushNotification.tokenId != null) {
                    title = context.getString(R.string.push_title_received_erc721)
                    text = context.getString(R.string.push_text_received_erc721, safeName)
                } else {
                    title = context.getString(R.string.push_title_received_erc20)
                    text = context.getString(R.string.push_text_received_erc20, safeName)
                }
                intent = txListIntent(safe)
            }
            is PushNotification.IncomingEther -> {
                title = context.getString(R.string.push_title_received_eth)
                val value = balanceFormatter.shortAmount(pushNotification.value.convertAmount(TokenRepository.ETH_TOKEN_INFO.decimals))
                text = context.getString(R.string.push_text_received_eth, safeName, value)
                intent = txListIntent(safe)
            }
        }

        return builder(title, text, CHANNEL_ID, intent)
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
            .setSmallIcon(R.drawable.ic_app_24dp)
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

    fun show(notification: Notification) {
        latestNotificationId += 1
        notificationManager.notify(latestNotificationId, notification)
    }

    fun show(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }

    fun hide(id: Int) {
        notificationManager.cancel(id)
    }

    fun hideAll() {
        latestNotificationId = -1
        notificationManager.cancelAll()
    }

    fun notificationsEnabled(): Boolean {
        var enabled = notificationManager.areNotificationsEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            enabled = enabled && channel?.importance != NotificationManager.IMPORTANCE_NONE

        }
        return enabled
    }

    private fun txDetailsIntent(safe: Safe, safeTxHash: String): PendingIntent {
        val intent = StartActivity.createIntent(context, safe, safeTxHash)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun txListIntent(safe: Safe): PendingIntent {
        val intent = StartActivity.createIntent(context, safe)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private const val LATEST_NOTIFICATION_ID = "prefs.int.latest_notification_id"
        private val VIBRATE_PATTERN = longArrayOf(0, 100, 50, 100)
        private const val LIGHT_COLOR = Color.MAGENTA
        private const val CHANNEL_ID = "channel_tx_notifications"
    }
}
