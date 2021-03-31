package io.gnosis.safe.notifications

import com.google.firebase.iid.FirebaseInstanceId
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeMetaData
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.utils.toSignatureString
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.notifications.models.PushNotification
import io.gnosis.safe.notifications.models.Registration
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import timber.log.Timber
import java.math.BigInteger
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class NotificationRepository(
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
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
        get() = preferencesManager.prefs.getString(KEY_DEVICE_UUID, null)
        set(value) {
            preferencesManager.prefs.edit {
                putString(KEY_DEVICE_UUID, value)
            }
        }

    private var registrationUpdateFailed: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_REGISTRATION_UPDATE_FAILED, false)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_REGISTRATION_UPDATE_FAILED, value)
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
        if (deviceUuid == null || registrationUpdateFailed) {
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

        val safes = safeRepository.getSafes().sortedBy { it.address.value }.map {
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
            timestamp = (System.currentTimeMillis() / 1000).toString()
        )

        registration.addSignatures(credentialsRepository.owners())

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
                    registrationUpdateFailed = false
                }
                .onFailure {
                    Timber.d("notification service registration failure")
                    deviceUuid = null
                    registrationUpdateFailed = true
                }
        }
    }

    suspend fun registerSafe(safe: Safe) {

        kotlin.runCatching {

            val token = getCloudMessagingToken()!!

            val registration = Registration(
                uuid = deviceUuid ?: generateUUID(),
                safes = listOf(safe.address.asEthereumAddressChecksumString()),
                cloudMessagingToken = token,
                bundle = BuildConfig.APPLICATION_ID,
                deviceType = "ANDROID",
                version = appVersion,
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                timestamp = (System.currentTimeMillis() / 1000).toString()
            )

            registration.addSignatures(credentialsRepository.owners())

            notificationService.register(registration)
        }
            .onSuccess {
                deviceUuid = it.uuid
                safeRepository.saveSafeMeta(SafeMetaData(safe.address, true))
                registrationUpdateFailed = false

                notificationManager.updateNotificationChannelGroupForSafe(safe)
            }
            .onFailure {
                registrationUpdateFailed = true
            }
    }

    suspend fun registerOwner(ownerKey: BigInteger) {

        kotlin.runCatching {

            val token = getCloudMessagingToken()!!

            val safes = safeRepository.getSafes().sortedBy { it.address.value }.map {
                it.address.asEthereumAddressChecksumString()
            }

            val registration = Registration(
                uuid = deviceUuid ?: generateUUID(),
                // safes are always added and never removed on the registration request
                safes = safes,
                cloudMessagingToken = token,
                bundle = BuildConfig.APPLICATION_ID,
                deviceType = "ANDROID",
                version = appVersion,
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                timestamp = (System.currentTimeMillis() / 1000).toString()
            )

            registration.buildAndAddSignature(ownerKey.toByteArray())

            notificationService.register(registration)
        }
            .onSuccess {
                deviceUuid = it.uuid
                registrationUpdateFailed = false
            }
            .onFailure {
                registrationUpdateFailed = true
            }
    }

    suspend fun unregisterOwners() {

        kotlin.runCatching {

            val token = getCloudMessagingToken()!!

            val safes = safeRepository.getSafes().sortedBy { it.address.value }.map {
                it.address.asEthereumAddressChecksumString()
            }

            val registration = Registration(
                uuid = deviceUuid ?: generateUUID(),
                // safes are always added and never removed on the registration request
                safes = safes,
                cloudMessagingToken = token,
                bundle = BuildConfig.APPLICATION_ID,
                deviceType = "ANDROID",
                version = appVersion,
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                timestamp = (System.currentTimeMillis() / 1000).toString()
            )

            // registration without signatures
            notificationService.register(registration)
        }
            .onSuccess {
                deviceUuid = it.uuid
                registrationUpdateFailed = false
            }
            .onFailure {
                registrationUpdateFailed = true
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

    private fun Registration.addSignatures(owners: List<Owner>) {
        if (owners.isNotEmpty()) {
            val registrationHash = hash().hexToByteArray()
            owners.forEach {
                val signature = credentialsRepository.signWithOwner(it, registrationHash).addHexPrefix()
                addSignature(signature)
            }
        }
    }

    companion object {
        private const val KEY_DEVICE_UUID = "prefs.string.device_uuid"
        private const val KEY_REGISTRATION_UPDATE_FAILED = "prefs.boolean.registration_update_failed"
    }
}

fun Registration.buildAndAddSignature(key: ByteArray) {
    val signature =
        KeyPair
            .fromPrivate(key)
            .sign(hash().hexToByteArray())
            .toSignatureString()
            .addHexPrefix()
    addSignature(signature)
}
