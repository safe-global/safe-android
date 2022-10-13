package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class SendAssetViewModel
@Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SendAssetState>(appDispatchers) {

    override fun initialState(): SendAssetState =
        SendAssetState(viewAction = null)

}

data class SendAssetState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
