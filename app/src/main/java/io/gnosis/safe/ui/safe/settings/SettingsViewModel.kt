package io.gnosis.safe.ui.safe.settings

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SettingsState>(appDispatchers) {

    override fun initialState() = SettingsState.SafeLoading(null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    if (safe == null)
                        SettingsState.NoActiveSafe(null)
                    else
                        SettingsState.ActiveSafe(safe, null)
                }
            }
        }
    }
}

sealed class SettingsState : BaseStateViewModel.State {

    data class SafeLoading(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SettingsState()

    data class ActiveSafe(
        val safe: Safe?,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SettingsState()

    data class NoActiveSafe(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SettingsState()
}

