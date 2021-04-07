package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.transactions.details.ConfirmConfirmation
import io.gnosis.safe.ui.transactions.details.TransactionDetailsFragmentDirections
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class OwnerListViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerListState>(appDispatchers) {

    override fun initialState() = OwnerListState(ViewAction.Loading(true))

    fun loadOwners(missingSigners: List<String>? = null) {
        safeLaunch {
            updateState {
                OwnerListState(viewAction = ViewAction.Loading(true))
            }
            val owners = credentialsRepository.owners().map { OwnerViewData.LocalOwner(it.address, it.name) }
            missingSigners?.let {
                val acceptedOwners = owners.filter { localOwner ->
                    missingSigners?.any {
                        localOwner.address.asEthereumAddressString() == it
                    } ?: false
                }
                updateState {
                    OwnerListState(viewAction = LocalOwners(acceptedOwners))
                }
            } ?: updateState {
                OwnerListState(viewAction = LocalOwners(owners))
            }

        }
    }

    fun removeOwner(owner: Solidity.Address) {
        safeLaunch {
            credentialsRepository.removeOwner(owner)
            notificationRepository.unregisterOwners()
            tracker.setNumKeysImported(credentialsRepository.ownerCount())
        }
    }


    //TODO: Move to SigningOwnerSelectionViewModel ?
    fun selectKeyForSigning(owner: Solidity.Address) {

        safeLaunch {
            if (settingsHandler.usePasscode) {
//                confirmationInProgress = true
                updateState {
                    OwnerListState(
                        ViewAction.NavigateTo(
                            TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToEnterPasscodeFragment()
                        )
                    )
                }
                updateState { OwnerListState(ViewAction.None) }
            } else {
                updateState { OwnerListState(ConfirmConfirmation(owner)) }
                updateState { OwnerListState(ViewAction.None) }
            }
        }
    }
}

data class OwnerListState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class LocalOwners(
    val owners: List<OwnerViewData>
) : BaseStateViewModel.ViewAction
