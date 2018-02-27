package pm.gnosis.heimdall.ui.settings.network

import android.content.Context
import io.reactivex.Single
import okhttp3.HttpUrl
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class NetworkSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : NetworkSettingsContract() {
    override fun loadIpfsUrl(): Single<String> {
        return Single.fromCallable {
            settingsRepository.getIpfsUrl()?.toString() ?: ""
        }
    }

    override fun updateIpfsUrl(url: String): Single<Result<String>> {
        return Single.fromCallable {
            if (url.isBlank()) {
                settingsRepository.setIpfsUrl(true, null, null)
            } else {
                parseUrl(url).let { settingsRepository.setIpfsUrl(it.isHttps, it.host, it.port) }
            }
            url
        }.mapToResult()
    }

    override fun loadRpcUrl(): Single<String> {
        return Single.fromCallable {
            settingsRepository.getEthereumRPCUrl()?.toString() ?: ""
        }
    }

    override fun updateRpcUrl(url: String): Single<Result<String>> {
        return Single.fromCallable {
            if (url.isBlank()) {
                settingsRepository.setEthereumRPCUrl(true, null, null)
            } else {
                parseUrl(url).let { settingsRepository.setEthereumRPCUrl(it.isHttps, it.host, it.port) }
            }
            url
        }.mapToResult()
    }

    private fun parseUrl(url: String): SettingsRepository.UrlOverride {
        if (!url.startsWith("https:") && !url.startsWith("http:")) {
            throw SimpleLocalizedException(context.getString(R.string.error_invalid_url_scheme))
        }
        val parsed = HttpUrl.parse(url) ?: throw SimpleLocalizedException(context.getString(R.string.error_invalid_url))
        if (parsed.pathSize() > 1 || !parsed.pathSegments().firstOrNull().isNullOrBlank()) {
            throw SimpleLocalizedException(context.getString(R.string.error_invalid_url_path))
        }
        val port = if (parsed.port() != HttpUrl.defaultPort(parsed.scheme())) parsed.port() else null
        return SettingsRepository.UrlOverride(parsed.isHttps, parsed.host(), port)
    }

    override fun loadProxyFactoryAddress(): Single<String> {
        return Single.fromCallable {
            settingsRepository.getProxyFactoryAddress().asEthereumAddressString()
        }
    }

    override fun updateProxyFactoryAddress(address: String): Single<Result<String>> {
        return Single.fromCallable {
            settingsRepository.setProxyFactoryAddress(address)
            address
        }
            .onErrorResumeNext { Single.error(SimpleLocalizedException(context.getString(R.string.invalid_ethereum_address))) }
            .mapToResult()
    }

    override fun loadSafeMasterCopyAddress(): Single<String> {
        return Single.fromCallable {
            settingsRepository.getSafeMasterCopyAddress().asEthereumAddressString()
        }
    }

    override fun updateSafeMasterCopyAddress(address: String): Single<Result<String>> {
        return Single.fromCallable {
            settingsRepository.setSafeMasterCopyAddress(address)
            address
        }
            .onErrorResumeNext { Single.error(SimpleLocalizedException(context.getString(R.string.invalid_ethereum_address))) }
            .mapToResult()
    }
}
