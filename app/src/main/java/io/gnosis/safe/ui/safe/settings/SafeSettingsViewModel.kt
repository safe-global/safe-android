package io.gnosis.safe.ui.safe.settings

import io.gnosis.data.models.Safe
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class SafeSettingsViewModel @Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSettingsState>(appDispatchers) {

    private val safeRepository = repositories.safeRepository()

    override fun initialState() = SafeSettingsState.SafeLoading(null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState { SafeSettingsState.SafeSettings(safe, null) }
            }
        }
    }

    fun removeSafe() {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            safe?.let {
                safeRepository.removeSafe(safe)
                updateState {
                    SafeSettingsState.SafeRemoved(
                        ViewAction.NavigateTo(
                            SafeSettingsFragmentDirections.actionSafeSettingsFragmentToNoSafeFragment()
                        )
                    )
                }
            }
        }
    }
}

sealed class SafeSettingsState : BaseStateViewModel.State {

    data class SafeLoading(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSettingsState()

    data class SafeSettings(
        val safe: Safe?,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSettingsState()

    data class SafeRemoved(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSettingsState()
}
