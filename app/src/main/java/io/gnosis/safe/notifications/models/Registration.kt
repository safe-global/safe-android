package io.gnosis.safe.notifications.models

import pm.gnosis.model.Solidity

data class RegisterNotificationRequest(
    val deviceId: String,
    val pushToken: String,
    val safes: List<Solidity.Address>,
    val appVersion: String,
    val platform: String,
    val device: String
)

data class UnregisterNotificationRequest(
    val deviceId: String,
    val safes: List<Solidity.Address>
)
