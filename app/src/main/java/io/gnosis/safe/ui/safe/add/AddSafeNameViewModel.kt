package io.gnosis.safe.ui.safe.add

import io.gnosis.data.models.Chain
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.ChainInfoRepository
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
    private val chainInfoRepository: ChainInfoRepository,
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    private val notificationManager: NotificationManager,
    appDispatchers: AppDispatchers,
    private val tracker: Tracker,
    private val notificationRepository: NotificationRepository
) : BaseStateViewModel<BaseStateViewModel.State>(appDispatchers) {

    fun submitAddressAndName(address: Solidity.Address, localName: String, chain: Chain) {
        safeLaunch {
            localName.takeUnless { it.isBlank() } ?: run {
                updateState { AddSafeNameState(ViewAction.ShowError(InvalidName())) }
                return@safeLaunch
            }
            updateState { AddSafeNameState(ViewAction.Loading(true)) }
            runCatching {
                val safe = Safe(address, localName.trim(), chain.chainId)
                safeRepository.saveSafe(safe)
                chainInfoRepository.save(chain)
                notificationRepository.registerSafes(safe)
                notificationManager.createNotificationChannelGroup(safe)
                safeRepository.setActiveSafe(safe)
            }.onFailure {
                updateState { AddSafeNameState(ViewAction.ShowError(it)) }
            }.onSuccess {
                tracker.logSafeAdded(chain.chainId)
                tracker.setNumSafes(safeRepository.getSafeCount())
                if (settingsHandler.showOwnerScreen && credentialsRepository.ownerCount() == 0) {
                    updateState { AddSafeNameState(ImportOwner) }
                } else {
                    updateState { AddSafeNameState(ViewAction.CloseScreen) }
                }
            }
        }
    }

    override fun initialState(): State = AddSafeNameState(ViewAction.Loading(false))

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend
}

class InvalidName : Throwable()

data class AddSafeNameState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object ImportOwner : BaseStateViewModel.ViewAction
