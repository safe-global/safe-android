package io.gnosis.safe.notifications.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.toHexString

@JsonClass(generateAdapter = true)
data class Registration(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "safes") val safes: List<String>,
    @Json(name = "cloudMessagingToken") val cloudMessagingToken: String,
    @Json(name = "bundle") val bundle: String,
    @Json(name = "version") val version: String,
    @Json(name = "deviceType") val deviceType: String = "ANDROID",
    @Json(name = "buildNumber") val buildNumber: String,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "signatures") val signatures: MutableList<String> = mutableListOf()
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
