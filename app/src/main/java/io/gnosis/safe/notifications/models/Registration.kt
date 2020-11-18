package io.gnosis.safe.notifications.models

data class FirebaseDevice(
    val uuid: String? = null,
    val safes: List<String>,
    val cloudMessagingToken: String,
    val bundle: String,
    val version: String,
    val deviceType: String = "ANDROID",
    val buildNumber: String,
    val timestamp: String? = null,
    val signatures: List<String>? = null
)
