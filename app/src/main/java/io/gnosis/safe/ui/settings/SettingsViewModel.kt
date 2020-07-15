package io.gnosis.safe.ui.settings

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

    override fun initialState() = SettingsState(null, ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    SettingsState(safe, ViewAction.None)
                }
            }
        }
    }
}

data class SettingsState(
    val safe: Safe?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
