package io.gnosis.safe.ui.settings.app.fiat

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.PublishViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import javax.inject.Inject

class AppFiatViewModel
@Inject constructor(
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : PublishViewModel<AppFiatFragmentState>(appDispatchers) {

    fun fetchSupportedFiatCodes() {
        safeLaunch {
            runCatching { settingsHandler.loadSupportedFiatCodes() }
                .onSuccess { supportedFiatCodes ->
                    updateState {
                        AppFiatFragmentState(FiatList(fiatCodes = supportedFiatCodes))
                    }
                }
                .onFailure {
                    updateState {
                        AppFiatFragmentState(BaseStateViewModel.ViewAction.ShowError(it))
                    }
                }
        }
    }

    fun fetchDefaultUserFiat() {
        safeLaunch {
            val userDefaultFiat = settingsHandler.userDefaultFiat
            updateState { AppFiatFragmentState(SelectFiat(userDefaultFiat)) }
        }
    }

    fun selectedFiatCodeChanged(fiatCode: String) {
        safeLaunch {
            settingsHandler.userDefaultFiat = fiatCode
            updateState { AppFiatFragmentState(SelectFiat(fiatCode)) }
        }
    }
}

data class FiatList(val fiatCodes: List<String>) : BaseStateViewModel.ViewAction
data class SelectFiat(val fiatCode: String) : BaseStateViewModel.ViewAction

data class AppFiatFragmentState(override var viewAction: BaseStateViewModel.ViewAction? = null) : BaseStateViewModel.State
