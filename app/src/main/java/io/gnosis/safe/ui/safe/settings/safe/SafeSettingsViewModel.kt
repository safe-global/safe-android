package io.gnosis.safe.ui.safe.settings.safe

import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

class SafeSettingsViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    private val ensRepository: EnsRepository,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSettingsState>(appDispatchers) {

    override fun initialState() = SafeSettingsState(null, null, null, ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                load(safe)
            }
        }
    }

    fun reload() {
        safeLaunch {
            load(safeRepository.getActiveSafe(), true)

        }
    }

    private suspend fun load(safe: Safe? = null, isRefreshing: Boolean = false) {
        updateState {
            val viewAction = if (isRefreshing) ViewAction.None else ViewAction.Loading(true)
            SafeSettingsState(null, null, null, viewAction)
        }
        val safeInfo = safe?.let { safeRepository.getSafeInfo(it.address) }
        val safeEnsName = runCatching { safe?.let { ensRepository.reverseResolve(it.address) } }
            .onFailure { Timber.e(it) }
            .getOrNull()
        updateState {
            SafeSettingsState(safe, safeInfo, safeEnsName, ViewAction.Loading(false))
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
                updateState { SafeSettingsState(safe, null, null, ViewAction.ShowError(it)) }
            }.onSuccess {
                updateState { SafeSettingsState(safe, null, null, SafeRemoved) }
                tracker.setNumSafes(safeRepository.getSafes().count())
            }
        }
    }
}

data class SafeSettingsState(
    val safe: Safe?,
    val safeInfo: SafeInfo?,
    val ensName: String?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object SafeRemoved : BaseStateViewModel.ViewAction
