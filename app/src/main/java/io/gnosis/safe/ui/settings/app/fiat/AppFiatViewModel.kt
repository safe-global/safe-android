package io.gnosis.safe.ui.settings.app.fiat

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.PublishViewModel

class AppFiatViewModel(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : PublishViewModel<AppFiatFragmentState>(appDispatchers) {

//    fun fetchFiats()
}

data class FiatList(val fiatCodes: List<String>) : BaseStateViewModel.ViewAction
data class SelectFiat(val fiatCode: String) : BaseStateViewModel.ViewAction

data class AppFiatFragmentState(override var viewAction: BaseStateViewModel.ViewAction? = null) : BaseStateViewModel.State
