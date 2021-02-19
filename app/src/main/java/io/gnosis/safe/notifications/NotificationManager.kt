package io.gnosis.safe.notifications

import android.app.*
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.R
import io.gnosis.safe.notifications.models.PushNotification
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import io.gnosis.safe.utils.formatForTxList
import kotlinx.coroutines.runBlocking
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import timber.log.Timber

class NotificationManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val balanceFormatter: BalanceFormatter,
    private val safeRepository: SafeRepository
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
//        createNotificationChannel(CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= 26) {

            runBlocking {
                safeRepository.getSafes().forEach { safe ->
                    createNotificationChannelGroup(safe.address.asEthereumAddressChecksumString(), safe.localName)
                    createNotificationChannel(safe.address.asEthereumAddressChecksumString())
                }
            }
        }

    }

    private var latestNotificationId: Int
        get() = preferencesManager.prefs.getInt(LATEST_NOTIFICATION_ID, -1)
        set(value) {
            preferencesManager.prefs.edit {
                putInt(LATEST_NOTIFICATION_ID, value)
            }
        }

    private fun createNotificationChannelGroup(id: String, name: CharSequence) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannelGroup(NotificationChannelGroup(id, name))
        }
    }

    private fun createNotificationChannel(channelId: String, importance: Int = NotificationManager.IMPORTANCE_HIGH) {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }
        val names = listOf<String>("Confirmation requests", "Executed transactions", "Incoming tokens", "Incoming Ether")

        names.forEach { name ->
//            val name = context.getString(R.string.channel_tx_notifications_name)
            val channel = NotificationChannel("$channelId.$name", name, importance)
            channel.description = name //context.getString(R.string.channel_tx_notifications_description)

            channel.enableLights(true)
            channel.lightColor = LIGHT_COLOR

            channel.enableVibration(true)
            channel.vibrationPattern = VIBRATE_PATTERN

            channel.group = channelId

            notificationManager.createNotificationChannel(channel)
        }

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
            is PushNotification.ConfirmationRequest -> {
                title = context.getString(R.string.push_title_confirmation_required)
                text = context.getString(R.string.push_text_confirmation_required, safeName)
                intent = txDetailsIntent(safe, pushNotification.safeTxHash)
            }
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
                val value = balanceFormatter.shortAmount(pushNotification.value.convertAmount(TokenRepository.NATIVE_CURRENCY_INFO.decimals))
                text = context.getString(R.string.push_text_received_eth, safeName, value)
                intent = txListIntent(safe)
            }
        }

        val name = when (pushNotification) {
            is PushNotification.ConfirmationRequest -> "Confirmation requests"
            is PushNotification.ExecutedTransaction -> "Executed transactions"
            is PushNotification.IncomingToken -> "Incoming tokens"
            is PushNotification.IncomingEther -> "Incoming Ether"
        }
        val channelId = safe.address.asEthereumAddressChecksumString() + "." + name
        Timber.i("---> channelId: $channelId")
        return builder(title, text, channelId, intent)
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
