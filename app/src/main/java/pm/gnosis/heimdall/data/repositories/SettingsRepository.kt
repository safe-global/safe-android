package pm.gnosis.heimdall.data.repositories

import okhttp3.HttpUrl
import okhttp3.Request
import pm.gnosis.model.Solidity


interface SettingsRepository {
    fun setEthereumRPCUrl(isHttps: Boolean, host: String?, port: Int?)
    fun getEthereumRPCUrl(): UrlOverride?

    fun getProxyFactoryAddress(): Solidity.Address
    fun setProxyFactoryAddress(address: Solidity.Address?)

    fun getSafeMasterCopyAddress(): Solidity.Address
    fun setSafeMasterCopyAddress(address: Solidity.Address?)

    fun getRecoveryExtensionMasterCopyAddress(): Solidity.Address
    fun getDailyLimitExtensionMasterCopyAddress(): Solidity.Address
    fun getCreateAndAddExtensionContractAddress(): Solidity.Address

    fun needsAuth(): Boolean

    data class UrlOverride(val isHttps: Boolean = true, val host: String, val port: Int? = null) {
        override fun toString(): String {
            return "${if (isHttps) "https" else "http"}://$host${if (port == null) "" else ":$port"}"
        }
    }
}

fun HttpUrl.override(override: SettingsRepository.UrlOverride): HttpUrl {
    return newBuilder()
        .scheme(if (override.isHttps) "https" else "http")
        .host(override.host)
        .apply { override.port?.let { port(it) } }
        .build()
}

fun Request.changeUrl(override: SettingsRepository.UrlOverride?): Request {
    override ?: return this
    return newBuilder().url(url().override(override)).build()
}
