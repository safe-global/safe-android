package io.gnosis.safe.notifications.models

import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.toHexString

data class Registration(
    val uuid: String,
    val safes: List<String>,
    val cloudMessagingToken: String,
    val bundle: String,
    val version: String,
    val deviceType: String = "ANDROID",
    val buildNumber: String,
    val timestamp: String? = null,
    val signatures: MutableList<String> = mutableListOf()
) {

    fun hash(): String {

        val stringToHash = StringBuilder().apply {
            append(PREFIX)
            append(timestamp)
            append(uuid)
            append(cloudMessagingToken)
            safes.forEach {
                append(it)
            }
        }.toString()

        return Sha3Utils.keccak(stringToHash.toByteArray()).toHexString().addHexPrefix()
    }

    fun addSignature(signature: String) {
        signatures.add(signature)
    }

    companion object {
        private const val PREFIX = "gnosis-safe"
    }
}
