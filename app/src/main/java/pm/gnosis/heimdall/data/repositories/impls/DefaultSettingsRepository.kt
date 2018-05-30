package pm.gnosis.heimdall.data.repositories.impls

import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSettingsRepository @Inject constructor(
    preferencesManager: PreferencesManager
) : SettingsRepository {

    companion object {
        private const val PREF_KEY_RPC_IS_HTTPS = "default_settings_repository_key.boolean.rpc_is_https"
        private const val PREF_KEY_RPC_HOST = "default_settings_repository_key.string.rpc_host"
        private const val PREF_KEY_RPC_PORT = "default_settings_repository_key.int.rpc_port"
        private const val PREF_KEY_PROXY_FACTORY_ADDRESS = "default_settings_repository_key.string.proxy_factory_address"
        private const val PREF_KEY_SAFE_MASTER_COPY_ADDRESS = "default_settings_repository_key.string.safe_master_copy_address"
    }

    private val preferences = preferencesManager.prefs

    override fun setEthereumRPCUrl(isHttps: Boolean, host: String?, port: Int?) {
        overrideUrlToPreferences(
            PREF_KEY_RPC_IS_HTTPS, PREF_KEY_RPC_HOST, PREF_KEY_RPC_PORT,
            isHttps, host, port
        )
    }

    override fun getEthereumRPCUrl(): SettingsRepository.UrlOverride? {
        return overrideUrlFromPreferences(PREF_KEY_RPC_IS_HTTPS, PREF_KEY_RPC_HOST, PREF_KEY_RPC_PORT)
    }

    override fun needsAuth() = true

    override fun getProxyFactoryAddress() =
        (preferences.getString(PREF_KEY_PROXY_FACTORY_ADDRESS, null) ?: BuildConfig.PROXY_FACTORY_ADDRESS).asEthereumAddress()!!

    override fun setProxyFactoryAddress(address: Solidity.Address?) {
        preferences.edit { putString(PREF_KEY_PROXY_FACTORY_ADDRESS, address?.asEthereumAddressString()) }
    }

    override fun getSafeMasterCopyAddress() =
        (preferences.getString(PREF_KEY_SAFE_MASTER_COPY_ADDRESS, null) ?: BuildConfig.SAFE_MASTER_COPY_ADDRESS).asEthereumAddress()!!

    override fun setSafeMasterCopyAddress(address: Solidity.Address?) {
        preferences.edit { putString(PREF_KEY_SAFE_MASTER_COPY_ADDRESS, address?.asEthereumAddressString()) }
    }

    override fun getRecoveryModuleMasterCopyAddress(): Solidity.Address =
        BuildConfig.RECOVERY_MODULE_MASTER_COPY_ADDRESS.asEthereumAddress()!!

    override fun getDailyLimitModuleMasterCopyAddress(): Solidity.Address =
        BuildConfig.DAILY_LIMIT_MODULE_MASTER_COPY_ADDRESS.asEthereumAddress()!!

    override fun getCreateAndAddModulesContractAddress(): Solidity.Address =
        BuildConfig.CREATE_AND_ADD_MODULES_LIB_ADDRESS.asEthereumAddress()!!

    private fun overrideUrlToPreferences(
        isHttpsKey: String, hostKey: String, portKey: String,
        isHttps: Boolean, host: String?, port: Int?
    ) {
        preferences.edit {
            putBoolean(isHttpsKey, isHttps)
            putString(hostKey, host)
            putInt(portKey, port ?: -1)
        }
    }

    private fun overrideUrlFromPreferences(isHttpsKey: String, hostKey: String, portKey: String): SettingsRepository.UrlOverride? {
        return preferences.let {
            val host: String? = it.getString(hostKey, null)
            if (host.isNullOrBlank()) {
                return null
            }
            val isHttps = it.getBoolean(isHttpsKey, true)
            val port = it.getInt(portKey, -1)
            SettingsRepository.UrlOverride(isHttps, host!!, if (port < 0) null else port)
        }
    }
}
