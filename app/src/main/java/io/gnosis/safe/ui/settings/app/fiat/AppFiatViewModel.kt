package io.gnosis.safe.ui.settings.app.fiat

import io.gnosis.data.repositories.SettingsRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.PublishViewModel
import javax.inject.Inject

class AppFiatViewModel
@Inject constructor(
    private val settingsRepository: SettingsRepository,
    appDispatchers: AppDispatchers
) : PublishViewModel<AppFiatFragmentState>(appDispatchers) {

    fun fetchSupportedFiatCodes() {
        safeLaunch {
            val supportedFiatCodes = settingsRepository.loadSupportedFiatCodes()
            updateState {
                AppFiatFragmentState(FiatList(fiatCodes = supportedFiatCodes))
            }
        }
    }

    fun fetchDefaultUserFiat() {}
}

data class FiatList(val fiatCodes: List<String>) : BaseStateViewModel.ViewAction
data class SelectFiat(val fiatCode: String) : BaseStateViewModel.ViewAction

data class AppFiatFragmentState(override var viewAction: BaseStateViewModel.ViewAction? = null) : BaseStateViewModel.State
