package pm.gnosis.heimdall.data.repositories.impls

import pm.gnosis.heimdall.app.core.BuildConfig
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.utils.edit
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsEthereumAddress
import pm.gnosis.utils.isValidEthereumAddress
import java.math.BigInteger
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
        private const val PREF_KEY_IPFS_IS_HTTPS = "default_settings_repository_key.boolean.ipfs_is_https"
        private const val PREF_KEY_IPFS_HOST = "default_settings_repository_key.string.ipfs_host"
        private const val PREF_KEY_IPFS_PORT = "default_settings_repository_key.int.ipfs_port"
        private const val PREF_KEY_PROXY_FACTORY_ADDRESS = "default_settings_repository_key.string.proxy_factory_address"
        private const val PREF_KEY_SAFE_MASTER_COPY_ADDRESS = "default_settings_repository_key.string.safe_master_copy_address"
    }

    private val preferences = preferencesManager.prefs

    override fun setEthereumRPCUrl(isHttps: Boolean, host: String?, port: Int?) {
        overrideUrlToPreferences(PREF_KEY_RPC_IS_HTTPS, PREF_KEY_RPC_HOST, PREF_KEY_RPC_PORT,
                isHttps, host, port)
    }

    override fun getEthereumRPCUrl(): SettingsRepository.UrlOverride? {
        return overrideUrlFromPreferences(PREF_KEY_RPC_IS_HTTPS, PREF_KEY_RPC_HOST, PREF_KEY_RPC_PORT)
    }

    override fun setIpfsUrl(isHttps: Boolean, host: String?, port: Int?) {
        overrideUrlToPreferences(PREF_KEY_IPFS_IS_HTTPS, PREF_KEY_IPFS_HOST, PREF_KEY_IPFS_PORT,
                isHttps, host, port)
    }

    override fun getIpfsUrl(): SettingsRepository.UrlOverride? {
        return overrideUrlFromPreferences(PREF_KEY_IPFS_IS_HTTPS, PREF_KEY_IPFS_HOST, PREF_KEY_IPFS_PORT)
    }

    override fun needsAuth(): Boolean {
        return true
    }

    override fun getProxyFactoryAddress(): BigInteger {
        return (preferences.getString(PREF_KEY_PROXY_FACTORY_ADDRESS, null) ?: BuildConfig.PROXY_FACTORY_ADDRESS).hexAsEthereumAddress()
    }

    @Throws(InvalidAddressException::class)
    override fun setProxyFactoryAddress(address: String?) {
        // Check if input address is null or blank reset to null, so that it will return the default
        val checkAddress = if (address.isNullOrBlank()) null else address
        if (checkAddress?.isValidEthereumAddress() == false) {
            throw InvalidAddressException()
        }
        preferences.edit {
            putString(PREF_KEY_PROXY_FACTORY_ADDRESS, checkAddress)
        }
    }

    override fun getSafeMasterCopyAddress(): BigInteger {
        return (preferences.getString(PREF_KEY_SAFE_MASTER_COPY_ADDRESS, null) ?: BuildConfig.SAFE_MASTER_COPY_ADDRESS).hexAsEthereumAddress()
    }

    @Throws(InvalidAddressException::class)
    override fun setSafeMasterCopyAddress(address: String?) {
        // Check if input address is null or blank reset to null, so that it will return the default
        val checkAddress = if (address.isNullOrBlank()) null else address
        if (checkAddress?.isValidEthereumAddress() == false) {
            throw InvalidAddressException()
        }
        preferences.edit {
            putString(PREF_KEY_SAFE_MASTER_COPY_ADDRESS, checkAddress)
        }
    }

    private fun overrideUrlToPreferences(isHttpsKey: String, hostKey: String, portKey: String,
                                         isHttps: Boolean, host: String?, port: Int?) {
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