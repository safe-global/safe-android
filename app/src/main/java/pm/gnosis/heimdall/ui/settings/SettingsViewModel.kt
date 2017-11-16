package pm.gnosis.heimdall.ui.settings

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.HttpUrl
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import java.lang.IllegalArgumentException
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val settingsRepository: SettingsRepository
) : SettingsContract() {
    override fun loadIpfsUrl(): Single<String> {
        return Single.fromCallable {
            settingsRepository.getIpfsUrl()?.toString() ?: ""
        }
    }

    override fun updateIpfsUrl(url: String): Completable {
        return Completable.fromAction {
            if (url.isBlank()) {
                settingsRepository.setIpfsUrl(true, null, null)
            } else {
                parseUrl(url).let { settingsRepository.setIpfsUrl(it.isHttps, it.host, it.port) }
            }
        }
    }

    override fun loadRpcUrl(): Single<String> {
        return Single.fromCallable {
            settingsRepository.getEthereumRPCUrl()?.toString() ?: ""
        }
    }

    override fun updateRpcUrl(url: String): Completable {
        return Completable.fromAction {
            if (url.isBlank()) {
                settingsRepository.setEthereumRPCUrl(true, null, null)
            } else {
                parseUrl(url).let { settingsRepository.setEthereumRPCUrl(it.isHttps, it.host, it.port) }
            }
        }
    }

    private fun parseUrl(url: String): SettingsRepository.UrlOverride {
        if (!url.startsWith("https:") && !url.startsWith("http:")) {
            throw LocalizedException(context.getString(R.string.error_invalid_url_scheme))
        }
        val parsed = HttpUrl.parse(url) ?: throw LocalizedException(context.getString(R.string.error_invalid_url))
        if (parsed.pathSize() > 1 || !parsed.pathSegments().firstOrNull().isNullOrBlank()) {
            throw LocalizedException(context.getString(R.string.error_invalid_url_path))
        }
        val port = if (parsed.port() != HttpUrl.defaultPort(parsed.scheme())) parsed.port() else null
        return SettingsRepository.UrlOverride(parsed.isHttps, parsed.host(), port)
    }

}