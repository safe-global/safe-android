package io.gnosis.safe.notifications.models

data class FirebaseDevice(
    val safes: List<String>,
    val cloudMessagingToken: String,
    val buildNumber: Int,
    val bundle: String,
    val version: String,
    val deviceType: String = "ANDROID",
    val uuid: String? = null
)
