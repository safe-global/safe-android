package io.gnosis.safe.ui.safe.settings

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SettingsState>(appDispatchers) {

    override fun initialState() = SettingsState(ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState(true) {
                    SettingsState(
                        if (safe == null)
                            ViewAction.NavigateTo(
                                SettingsFragmentDirections.actionSettingsFragmentToNoSafeFragment()
                            )
                        else
                            ViewAction.UpdateActiveSafe(safe)
                    )
                }
            }
        }
    }

}

data class SettingsState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

