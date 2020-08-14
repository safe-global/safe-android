package io.gnosis.safe.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.notifications.models.PushNotification
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.utils.formatForTxList
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit

class NotificationManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
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
            is PushNotification.NewConfirmation -> {
                title = context.getString(R.string.push_title_new_confirmation)
                text = context.getString(R.string.push_text_new_confirmation, safeName, pushNotification.owner.formatForTxList())
            }
            is PushNotification.ExecutedTransaction -> {
                if (pushNotification.failed) {
                    title = context.getString(R.string.push_title_failed)
                    text = context.getString(R.string.push_text_failed, safeName)
                } else {
                    title = context.getString(R.string.push_title_executed)
                    text = context.getString(R.string.push_text_executed, safeName)
                }
                intent = txDetailsIntent(pushNotification.safeTxHash)
            }
            is PushNotification.IncomingToken -> {
                if (pushNotification.tokenId != null) {
                    title = context.getString(R.string.push_title_received_erc721)
                    text = context.getString(R.string.push_text_received_erc721, safeName)
                } else {
                    title = context.getString(R.string.push_title_received_erc20)
                    text = context.getString(R.string.push_text_received_erc20, safeName)
                }
                intent = txListIntent(pushNotification.txHash)
            }
            is PushNotification.IncomingEther -> {
                title = context.getString(R.string.push_title_received_eth)
                text = context.getString(R.string.push_text_received_eth, safeName)
                intent = txListIntent(pushNotification.txHash)
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

    fun show(notification: Notification) {
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

    private fun txDetailsIntent(safeTxHash: String): PendingIntent =
        NavDeepLinkBuilder(context)
            .setComponentName(StartActivity::class.java)
            .setGraph(R.navigation.main_nav)
            .setDestination(R.id.transactionDetailsFragment)
            .setArguments(Bundle().apply {
                putString("txId", safeTxHash)
            })
            .createPendingIntent()

    private fun txListIntent(txHash: String): PendingIntent =
        NavDeepLinkBuilder(context)
            .setComponentName(StartActivity::class.java)
            .setGraph(R.navigation.main_nav)
            .setDestination(R.id.transactionListFragment)
            .setArguments(Bundle().apply {
                putString("txHash", txHash)
            })
            .createPendingIntent()


    companion object {
        private const val LATEST_NOTIFICATION_ID = "prefs.int.latest_notification_id"
        private val VIBRATE_PATTERN = longArrayOf(0, 100, 50, 100)
        private const val LIGHT_COLOR = Color.MAGENTA
        private const val CHANNEL_ID = "channel_tx_notifications"
    }
}
