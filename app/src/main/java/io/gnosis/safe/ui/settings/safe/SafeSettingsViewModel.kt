package io.gnosis.safe.ui.settings.safe

import androidx.lifecycle.viewModelScope
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationManager
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.SettingsFragmentDirections
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SafeSettingsViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    private val ensRepository: EnsRepository,
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationManager: NotificationManager,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSettingsState>(appDispatchers) {

    override fun initialState() = SafeSettingsState(null, null, null, listOf(), ViewAction.Loading(true))

    init {
        viewModelScope.launch {
            safeRepository.activeSafeFlow().collect { safe ->
                safeLaunch {
                    updateState { SafeSettingsState(null, null, null, listOf(), ViewAction.UpdateActiveSafe(safe)) }
                    load(safe)
                }
            }
        }
    }

    fun reload() {
        safeLaunch {
            updateState { SafeSettingsState(null, null, null, listOf(), ViewAction.None) }
            load(safeRepository.getActiveSafe())
        }
    }

    private suspend fun load(safe: Safe?) {

        updateState { SafeSettingsState(null, null, null, listOf(), ViewAction.Loading(true)) }

        val safeInfo = safe?.let { safeRepository.getSafeInfo(it) }

        val localOwners = credentialsRepository.owners()

        val safeEnsName = runCatching { safe?.let { ensRepository.reverseResolve(it.address, safe.chain) } }
            .onFailure { Timber.e(it) }
            .getOrNull()

        updateState {
            SafeSettingsState(safe, safeInfo, safeEnsName, localOwners, ViewAction.Loading(false))
        }
    }

    fun isLoading(): Boolean {
        val viewAction = (state.value as SafeSettingsState).viewAction
        return (viewAction is ViewAction.Loading && viewAction.isLoading)
    }

    fun removeSafe() {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            safe?.let {
                runCatching {
                    safeRepository.removeSafe(safe)
                    val safes = safeRepository.getSafes()
                    if (safes.isEmpty()) {
                        safeRepository.clearActiveSafe()
                    } else {
                        safeRepository.setActiveSafe(safes.first())
                    }
                }.onFailure {
                    updateState { SafeSettingsState(safe, null, null, listOf(), ViewAction.ShowError(it)) }
                }.onSuccess {
                    updateState { SafeSettingsState(safe, null, null, listOf(), SafeRemoved) }
                    tracker.logSafeRemoved(safe.chainId)
                    tracker.setNumSafes(safeRepository.getSafeCount())
                    notificationRepository.unregisterSafe(safe.chainId, safe.address)
                    notificationManager.deleteNotificationChannelGroup(safe)
                }
            }
        }
    }

    fun openAdvancedSettings() {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()!!
            updateState {
                SafeSettingsState(
                    safe,
                    null,
                    null,
                    listOf(),
                    ViewAction.NavigateTo(
                        SettingsFragmentDirections.actionSettingsFragmentToAdvancedSafeSettingsFragment(safe.chain)
                    )
                )
            }
            updateState {
                SafeSettingsState(
                    null,
                    null,
                    null,
                    listOf(),
                    ViewAction.None
                )
            }
        }
    }
}

data class SafeSettingsState(
    val safe: Safe?,
    val safeInfo: SafeInfo?,
    val ensName: String?,
    val localOwners: List<Owner>,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object SafeRemoved : BaseStateViewModel.ViewAction
