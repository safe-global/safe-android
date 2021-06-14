package io.gnosis.safe.ui.safe.add

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationManager
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.model.Solidity
import javax.inject.Inject

class AddSafeNameViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val notificationRepository: NotificationRepository,
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    private val notificationManager: NotificationManager,
    appDispatchers: AppDispatchers,
    private val tracker: Tracker
) : BaseStateViewModel<BaseStateViewModel.State>(appDispatchers) {

    fun submitAddressAndName(address: Solidity.Address, localName: String) {
        safeLaunch {
            localName.takeUnless { it.isBlank() } ?: run {
                updateState { AddSafeNameState(ViewAction.ShowError(InvalidName())) }
                return@safeLaunch
            }
            updateState { AddSafeNameState(ViewAction.Loading(true)) }
            runCatching {
                val safe = Safe(address, localName.trim())
                safeRepository.saveSafe(safe)
                notificationRepository.registerOwners(safe)
                notificationManager.createNotificationChannelGroup(safe)
                safeRepository.setActiveSafe(safe)
            }.onFailure {
                updateState { AddSafeNameState(ViewAction.ShowError(it)) }
            }.onSuccess {
                tracker.setNumSafes(safeRepository.getSafeCount())
                if(settingsHandler.showOwnerScreen && credentialsRepository.ownerCount() == 0) {
                    updateState { AddSafeNameState(ImportOwner) }
                } else {
                    updateState { AddSafeNameState(ViewAction.CloseScreen) }
                }
            }
        }
    }

    override fun initialState(): State = AddSafeNameState(ViewAction.Loading(false))
}

class InvalidName : Throwable()

data class AddSafeNameState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object ImportOwner : BaseStateViewModel.ViewAction
