package io.gnosis.safe.ui.safe.settings.app

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class AppSettingsViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
): BaseStateViewModel<AppSettingsState>(appDispatchers) {

    override fun initialState() = AppSettingsState(null)

}

data class AppSettingsState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
