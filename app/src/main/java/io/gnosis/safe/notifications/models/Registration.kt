package io.gnosis.safe.notifications.models

import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.toHexString

data class Registration(
    val uuid: String? = null,
    val safes: List<String>,
    val cloudMessagingToken: String,
    val bundle: String,
    val version: String,
    val deviceType: String = "ANDROID",
    val buildNumber: String,
    val timestamp: String? = null,
    val signatures: List<String>? = null
) {

    fun hash(): String {

        val stringToHash = StringBuilder().apply {
            append(PREFIX)
            append(uuid)
            safes.forEach {
                append(it)
            }
            append(cloudMessagingToken)
            append(bundle)
            append(version)
            append(deviceType)
            append(buildNumber)
            append(timestamp)
        }.toString()

        return Sha3Utils.keccak(stringToHash.toByteArray()).toHexString().addHexPrefix()
    }

    companion object {
        private const val PREFIX = "gnosis-safe"
    }
}
