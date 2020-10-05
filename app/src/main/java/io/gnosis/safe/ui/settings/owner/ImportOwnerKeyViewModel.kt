package io.gnosis.safe.ui.settings.owner

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class ImportOwnerKeyViewModel
@Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ImportOwnerKeyState>(appDispatchers) {

    override fun initialState(): ImportOwnerKeyState = ImportOwnerKeyState.AwaitingInput
}

sealed class ImportOwnerKeyState : BaseStateViewModel.State {
    override var viewAction: BaseStateViewModel.ViewAction? = null

    object AwaitingInput : ImportOwnerKeyState()
}
