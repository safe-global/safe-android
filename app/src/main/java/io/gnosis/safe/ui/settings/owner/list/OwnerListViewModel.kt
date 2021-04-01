package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.model.Solidity
import javax.inject.Inject

class OwnerListViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerListState>(appDispatchers) {

    override fun initialState() = OwnerListState(ViewAction.Loading(true))

    fun loadOwners() {
        safeLaunch {
            updateState {
                OwnerListState(viewAction = ViewAction.Loading(true))
            }
            val owners = credentialsRepository.owners().map { OwnerViewData.LocalOwner(it.address, it.name) }
            updateState {
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
}

data class OwnerListState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class LocalOwners(
    val owners: List<OwnerViewData>
): BaseStateViewModel.ViewAction
