package io.gnosis.safe.notifications

import com.google.firebase.iid.FirebaseInstanceId
import io.gnosis.data.models.SafeMetaData
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.utils.toSignatureString
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.notifications.models.PushNotification
import io.gnosis.safe.notifications.models.Registration
import io.gnosis.safe.utils.OwnerCredentialsRepository
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import timber.log.Timber
import java.math.BigInteger
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class NotificationRepository(
    private val safeRepository: SafeRepository,
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationService: NotificationServiceApi,
    private val notificationManager: NotificationManager
) {

    //FIXME: workaround for versioning validation on notification service
    private val appVersion: String = BuildConfig.VERSION_NAME
        .replace("(\\d*)\\.(\\d*)\\.(\\d*)(.*)".toRegex()) {
            "${it.groupValues[1]}.${it.groupValues[2]}.${it.groupValues[3]}"
        }

    private var deviceUuid: String?
        get() = preferencesManager.prefs.getString(DEVICE_UUID, null)
        set(value) {
            preferencesManager.prefs.edit {
                putString(DEVICE_UUID, value)
            }
        }

    suspend fun handlePushNotification(pushNotification: PushNotification) {
        val safe = safeRepository.getSafes().find { it.address == pushNotification.safe }
        if (safe == null) {
            unregisterSafe(pushNotification.safe)
        } else {
            val notification = notificationManager.builder(safe, pushNotification).build()
            notificationManager.show(notification)
        }
    }

    fun checkPermissions(): Boolean = notificationManager.notificationsEnabled()

    fun clearNotifications() {
        notificationManager.hideAll()
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

        val safes = safeRepository.getSafes().map {
            it.address.asEthereumAddressChecksumString()
        }

        val registration = Registration(
            uuid = deviceUuid ?: generateUUID(),
            safes = safes,
            cloudMessagingToken = token,
            bundle = BuildConfig.APPLICATION_ID,
            deviceType = "ANDROID",
            version = appVersion,
            buildNumber = BuildConfig.VERSION_CODE.toString(),
            timestamp = System.currentTimeMillis().toString()
        )

        if (ownerCredentialsRepository.hasCredentials()) {

            val signature =
                KeyPair
                    .fromPrivate(ownerCredentialsRepository.retrieveCredentials()!!.key)
                    .sign(registration.hash().toByteArray())
                    .toSignatureString()

            registration.addSignature(signature)
        }


        if (safes.isNotEmpty()) {
            kotlin.runCatching {
                notificationService.register(registration)
            }
                .onSuccess {
                    Timber.d("notification service registration success")
                    deviceUuid = it.uuid
                    it.safes.forEach { safeAddressString ->
                        safeRepository.saveSafeMeta(SafeMetaData(safeAddressString.asEthereumAddress()!!, true))
                    }
                }
                .onFailure {
                    Timber.d("notification service registration failure")
                    deviceUuid = null
                }
        }
    }


    suspend fun registerSafe(safeAddress: Solidity.Address) {

        kotlin.runCatching {

            val token = getCloudMessagingToken()!!

            val registration = Registration(
                uuid = deviceUuid ?: generateUUID(),
                safes = listOf(safeAddress.asEthereumAddressChecksumString()),
                cloudMessagingToken = token,
                bundle = BuildConfig.APPLICATION_ID,
                deviceType = "ANDROID",
                version = appVersion,
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                timestamp = System.currentTimeMillis().toString()
            )

            if (ownerCredentialsRepository.hasCredentials()) {

                val signature =
                    KeyPair
                        .fromPrivate(ownerCredentialsRepository.retrieveCredentials()!!.key)
                        .sign(registration.hash().toByteArray())
                        .toSignatureString()

                registration.addSignature(signature)
            }

            notificationService.register(registration)
        }
            .onSuccess {
                deviceUuid = it.uuid
                safeRepository.saveSafeMeta(SafeMetaData(safeAddress, true))
            }
    }

    suspend fun registerOwner(ownerKey: BigInteger) {

        kotlin.runCatching {

            val token = getCloudMessagingToken()!!

            val registration = Registration(
                uuid = deviceUuid ?: generateUUID(),
                // safes are always added and never removed on the registration request
                safes = listOf(),
                cloudMessagingToken = token,
                bundle = BuildConfig.APPLICATION_ID,
                deviceType = "ANDROID",
                version = appVersion,
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                timestamp = System.currentTimeMillis().toString()
            )

            val signature =
                KeyPair
                    .fromPrivate(ownerKey)
                    .sign(registration.hash().toByteArray())
                    .toSignatureString()

            registration.addSignature(signature)

            notificationService.register(registration)
        }
            .onSuccess {
                deviceUuid = it.uuid
            }
    }

    suspend fun unregisterOwner() {

        kotlin.runCatching {

            val token = getCloudMessagingToken()!!

            val registration = Registration(
                uuid = deviceUuid ?: generateUUID(),
                // safes are always added and never removed on the registration request
                safes = listOf(),
                cloudMessagingToken = token,
                bundle = BuildConfig.APPLICATION_ID,
                deviceType = "ANDROID",
                version = appVersion,
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                timestamp = System.currentTimeMillis().toString()
            )

            // registration without signatures
            notificationService.register(registration)
        }
            .onSuccess {
                deviceUuid = it.uuid
            }
    }

    suspend fun unregister() {
        kotlin.runCatching {
            deviceUuid?.let {
                notificationService.unregister(it)
            }
        }
    }

    suspend fun unregisterSafe(safeAddress: Solidity.Address) {
        // no need to update safe meta because on safe removal safe meta entry will be also deleted
        kotlin.runCatching {
            deviceUuid?.let {
                notificationService.unregisterSafe(it, safeAddress.asEthereumAddressChecksumString())
            }
        }
    }

    private suspend fun getCloudMessagingToken() = suspendCoroutine<String?> { cont ->
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.e(task.exception)
                    cont.resumeWithException(task.exception!!)
                } else {
                    // Get new Instance ID token
                    val token = task.result?.token
                    Timber.d("Firebase token: $token")
                    cont.resume(token)
                }
            }
    }

    private fun generateUUID(): String {
        return UUID.randomUUID().toString().toLowerCase()
    }

    companion object {
        private const val DEVICE_UUID = "prefs.string.device_uuid"
    }
}
