package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import pm.gnosis.svalinn.common.PreferencesManager

class SettingsRepository(
    private val gatewayApi: GatewayApi,
    private val preferencesManager: PreferencesManager
) {

    suspend fun loadSupportedFiatCodes(): List<String> = gatewayApi.loadSupportedCurrencies()

    suspend fun getUserDefaultFiat(): String = preferencesManager.prefs.getString(USER_DEFAULT_FIAT, "USD") ?: "USD"

    companion object {
        const val USER_DEFAULT_FIAT = "USER_DEFAULT_FIAT"
    }
}
