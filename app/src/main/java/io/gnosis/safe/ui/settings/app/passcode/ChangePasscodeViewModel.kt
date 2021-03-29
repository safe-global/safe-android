package io.gnosis.safe.ui.settings.app.passcode

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class ChangePasscodeViewModel
@Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ChangePasscodeViewModel.ChangePasscodeState>(appDispatchers) {

    override fun initialState(): ChangePasscodeState = ChangePasscodeState(viewAction = null)

    data class ChangePasscodeState(override var viewAction: ViewAction?) : State

}
