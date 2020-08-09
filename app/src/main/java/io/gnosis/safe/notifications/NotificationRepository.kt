package io.gnosis.safe.notifications

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.RemoteMessage
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.notifications.models.FirebaseDevice
import io.gnosis.safe.notifications.models.Notification
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class NotificationRepository(
    private val safeRepository: SafeRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationService: NotificationServiceApi,
    private val notificationManager: NotificationManager
) {

    private var deviceUuid: String?
        get() = preferencesManager.prefs.getString(DEVICE_UUID, "null")
        set(value) {
            preferencesManager.prefs.edit {
                putString(DEVICE_UUID, value)
            }
        }

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

    suspend fun register() {
        if (deviceUuid == null) {
            kotlin.runCatching {
                getCloudMessagingToken()
            }
                .onSuccess {
                    it?.let {
                        register(it)
                    }
                }

        }
    }

    suspend fun register(token: String) {
        kotlin.runCatching {
            val safes = safeRepository.getSafes().map {
                it.address.asEthereumAddressChecksumString()
            }

            notificationService.register(
                FirebaseDevice(
                    safes,
                    token,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.APPLICATION_ID,
                    BuildConfig.VERSION_NAME,
                    "ANDROID",
                    deviceUuid
                )
            )
        }
            .onSuccess {
                deviceUuid = it.uuid
            }
            .onFailure {
                deviceUuid = null
            }
    }


    suspend fun registerSafe(safe: Safe) {
        kotlin.runCatching {
            deviceUuid?.let {
                val token = getCloudMessagingToken()
                token?.let {
                    notificationService.register(
                        FirebaseDevice(
                            listOf(safe.address.asEthereumAddressChecksumString()),
                            token,
                            BuildConfig.VERSION_CODE,
                            BuildConfig.APPLICATION_ID,
                            BuildConfig.VERSION_NAME,
                            "ANDROID",
                            deviceUuid
                        )
                    )
                }
            }
        }
    }

    suspend fun unregister() {
        kotlin.runCatching {
            deviceUuid?.let {
                notificationService.unregister(it)
            }
        }
    }

    suspend fun unregisterSafe(safe: Safe) {
        kotlin.runCatching {
            deviceUuid?.let {
                notificationService.unregisterSafe(it, safe.address.asEthereumAddressChecksumString())
            }
        }
    }

    private suspend fun getCloudMessagingToken() = suspendCoroutine<String?> { cont ->
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.e(task.exception)
                    cont.resumeWithException(task.exception!!)
                }
                // Get new Instance ID token
                val token = task.result?.token
                Timber.d("Firebase token: $token")
                cont.resume(token)
            })
    }

    companion object {
        private const val DEVICE_UUID = "prefs.string.device_uuid"
    }
}
