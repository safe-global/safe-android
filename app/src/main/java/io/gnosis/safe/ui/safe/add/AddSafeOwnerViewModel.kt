package io.gnosis.safe.ui.safe.add

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class AddSafeOwnerViewModel
@Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<BaseStateViewModel.State>(appDispatchers) {

    override fun initialState(): State = AddSafeOwnerState(ViewAction.Loading(false))
}

data class AddSafeOwnerState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
