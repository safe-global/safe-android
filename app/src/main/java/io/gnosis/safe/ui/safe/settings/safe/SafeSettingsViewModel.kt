package io.gnosis.safe.ui.safe.settings.safe

import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
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

    override fun initialState() = SafeSettingsState(null, null, ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                val safeInfo = safe?.let { safeRepository.getSafeInfo(it.address) }
                updateState {
                    SafeSettingsState(safe, safeInfo, ViewAction.None)
                }
            }
        }
    }

    fun removeSafe() {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            runCatching {
                safe?.let {
                    safeRepository.removeSafe(safe)
                    val safes = safeRepository.getSafes()
                    if (safes.isEmpty()) {
                        safeRepository.clearActiveSafe()
                    } else {
                        safeRepository.setActiveSafe(safes.first())
                    }
                }
            }.onFailure {
                updateState { SafeSettingsState(safe, null, ViewAction.ShowError(it)) }
            }.onSuccess {
                updateState { SafeSettingsState(safe, null, SafeRemoved) }
                tracker.setNumSafes(safeRepository.getSafes().count())
            }
        }
    }
}

data class SafeSettingsState(
    val safe: Safe?,
    val safeInfo: SafeInfo?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object SafeRemoved : BaseStateViewModel.ViewAction
