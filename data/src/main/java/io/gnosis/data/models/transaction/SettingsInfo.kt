package io.gnosis.data.models.transaction

import pm.gnosis.model.Solidity

enum class SettingsInfoType {
    SET_FALLBACK_HANDLER, ADD_OWNER, REMOVE_OWNER, SWAP_OWNER, CHANGE_THRESHOLD, CHANGE_IMPLEMENTATION, ENABLE_MODULE, DISABLE_MODULE
}

sealed class SettingsInfo(val type: SettingsInfoType) {

    data class SetFallbackHandler(val handler: Solidity.Address) : SettingsInfo(SettingsInfoType.SET_FALLBACK_HANDLER)
    data class AddOwner(val owner: Solidity.Address, val threshold: Long) : SettingsInfo(SettingsInfoType.ADD_OWNER)
    data class RemoveOwner(val owner: Solidity.Address, val threshold: Long) : SettingsInfo(SettingsInfoType.REMOVE_OWNER)
    data class SwapOwner(val oldOwner: Solidity.Address, val newOwner: Solidity.Address) : SettingsInfo(SettingsInfoType.SWAP_OWNER)
    data class ChangeThreshold(val threshold: Long) : SettingsInfo(SettingsInfoType.CHANGE_THRESHOLD)
    data class ChangeImplementation(val implementation: Solidity.Address) : SettingsInfo(SettingsInfoType.CHANGE_IMPLEMENTATION)
    data class EnableModule(val module: Solidity.Address) : SettingsInfo(SettingsInfoType.ENABLE_MODULE)
    data class DisableModule(val module: Solidity.Address) : SettingsInfo(SettingsInfoType.DISABLE_MODULE)
}
