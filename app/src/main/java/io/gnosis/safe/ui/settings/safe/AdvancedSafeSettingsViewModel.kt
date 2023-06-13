package io.gnosis.safe.ui.settings.safe

import io.gnosis.data.models.Chain
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.model.Solidity
import javax.inject.Inject

class AdvancedSafeSettingsViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<AdvancedSafeSettingsState>(appDispatchers) {

    override fun initialState(): AdvancedSafeSettingsState = AdvancedSafeSettingsState()

    fun load() {
        safeLaunch {
            safeRepository.getActiveSafe()?.let { safe ->
                val safeInfo = safeRepository.getSafeInfo(safe)
                updateState {
                    AdvancedSafeSettingsState(
                        isLoading = false,
                        viewAction = LoadSafeInfo(safe.chain, safeInfo)
                    )
                }
            }
        }
    }

    fun isDefaultFallbackHandler(fallbackHandle: Solidity.Address): Boolean =
        SafeRepository.DEFAULT_FALLBACK_HANDLER == fallbackHandle

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled() = settingsHandler.chainPrefixCopy
}

data class LoadSafeInfo(
    val chain: Chain,
    val safeInfo: SafeInfo
) : BaseStateViewModel.ViewAction

data class AdvancedSafeSettingsState(
    val isLoading: Boolean = true,
    override var viewAction: BaseStateViewModel.ViewAction? = BaseStateViewModel.ViewAction.None
) : BaseStateViewModel.State
