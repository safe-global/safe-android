package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.models.AddressInfoExtended

enum class SettingsInfoType {
    @Json(name = "SET_FALLBACK_HANDLER")
    SET_FALLBACK_HANDLER,
    @Json(name = "ADD_OWNER")
    ADD_OWNER,
    @Json(name = "REMOVE_OWNER")
    REMOVE_OWNER,
    @Json(name = "SWAP_OWNER")
    SWAP_OWNER,
    @Json(name = "CHANGE_THRESHOLD")
    CHANGE_THRESHOLD,
    @Json(name = "CHANGE_IMPLEMENTATION")
    CHANGE_IMPLEMENTATION,
    @Json(name = "ENABLE_MODULE")
    ENABLE_MODULE,
    @Json(name = "DISABLE_MODULE")
    DISABLE_MODULE
}

sealed class SettingsInfo(
    @Json(name = "type") val type: SettingsInfoType
) {
    @JsonClass(generateAdapter = true)
    data class SetFallbackHandler(
        @Json(name = "handler") val handler: AddressInfoExtended
    ) : SettingsInfo(SettingsInfoType.SET_FALLBACK_HANDLER)

    @JsonClass(generateAdapter = true)
    data class AddOwner(
        @Json(name = "owner") val owner: AddressInfoExtended,
        @Json(name = "threshold") val threshold: Long
    ) : SettingsInfo(SettingsInfoType.ADD_OWNER)

    @JsonClass(generateAdapter = true)
    data class RemoveOwner(
        @Json(name = "owner") val owner: AddressInfoExtended,
        @Json(name = "threshold") val threshold: Long
    ) : SettingsInfo(SettingsInfoType.REMOVE_OWNER)

    @JsonClass(generateAdapter = true)
    data class SwapOwner(
        @Json(name = "oldOwner") val oldOwner: AddressInfoExtended,
        @Json(name = "newOwner") val newOwner: AddressInfoExtended
    ) : SettingsInfo(SettingsInfoType.SWAP_OWNER)

    @JsonClass(generateAdapter = true)
    data class ChangeThreshold(
        @Json(name = "threshold") val threshold: Long
    ) : SettingsInfo(SettingsInfoType.CHANGE_THRESHOLD)

    @JsonClass(generateAdapter = true)
    data class ChangeImplementation(
        @Json(name = "implementation") val implementation: AddressInfoExtended
    ) : SettingsInfo(SettingsInfoType.CHANGE_IMPLEMENTATION)

    @JsonClass(generateAdapter = true)
    data class EnableModule(
        @Json(name = "module") val module: AddressInfoExtended
    ) : SettingsInfo(SettingsInfoType.ENABLE_MODULE)

    @JsonClass(generateAdapter = true)
    data class DisableModule(
        @Json(name = "module") val module: AddressInfoExtended
    ) : SettingsInfo(SettingsInfoType.DISABLE_MODULE)
}
