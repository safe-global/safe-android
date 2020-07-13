package io.gnosis.safe.ui.safe.share

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class ShareSafeViewModel
@Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ShareSafeState>(appDispatchers) {

    override fun initialState(): ShareSafeState = ShareSafeState()
}

data class ShareSafeState(
    override var viewAction: BaseStateViewModel.ViewAction? = BaseStateViewModel.ViewAction.Loading(true)
) : BaseStateViewModel.State
