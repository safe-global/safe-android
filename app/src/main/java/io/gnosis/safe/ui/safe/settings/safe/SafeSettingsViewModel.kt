package io.gnosis.safe.ui.safe.settings.safe

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class SafeSettingsViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSettingsState>(appDispatchers) {

    override fun initialState() = SafeSettingsState(ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState(true) {
                    SafeSettingsState(ViewAction.UpdateActiveSafe(safe))
                }
            }
        }
    }

    fun removeSafe() {
        safeLaunch {
            runCatching {
                val safe = safeRepository.getActiveSafe()
                safe?.let {
                    safeRepository.removeSafe(safe)
                    safeRepository.clearActiveSafe()
                }
            }.onFailure {
                updateState { SafeSettingsState(ViewAction.ShowError(it)) }
            }.onSuccess {
                updateState { SafeSettingsState(SafeRemoved) }
                tracker.setNumSafes(safeRepository.getSafes().count())
            }
        }
    }
}

data class SafeSettingsState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object SafeRemoved : BaseStateViewModel.ViewAction
