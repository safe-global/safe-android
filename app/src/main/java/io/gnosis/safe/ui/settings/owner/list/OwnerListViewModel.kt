package io.gnosis.safe.ui.settings.owner.list

import io.gnosis.data.models.Owner
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class OwnerListViewModel
@Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerListState>(appDispatchers) {

    override fun initialState() = OwnerListState(ViewAction.Loading(true))

}

data class OwnerListState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class LocalOwners(
    val owners: List<Owner>
): BaseStateViewModel.ViewAction
