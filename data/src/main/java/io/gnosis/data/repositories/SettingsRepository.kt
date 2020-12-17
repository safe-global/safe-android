package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit

class SettingsRepository(
    private val gatewayApi: GatewayApi,
    private val preferencesManager: PreferencesManager
) {

    suspend fun loadSupportedFiatCodes(): List<String> = gatewayApi.loadSupportedCurrencies().sorted()

    suspend fun getUserDefaultFiat(): String = preferencesManager.prefs.getString(USER_DEFAULT_FIAT, "USD") ?: "USD"

    suspend fun updateUserFiat(fiatCode: String) {
            preferencesManager.prefs.edit { putString(USER_DEFAULT_FIAT, fiatCode) }
    }

    companion object {
        const val USER_DEFAULT_FIAT = "USER_DEFAULT_FIAT"
    }
}
