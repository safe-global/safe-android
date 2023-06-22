package io.gnosis.safe.ui.transactions.execution

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class TxReviewViewModel
@Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TxReviewState>(appDispatchers) {

    override fun initialState() = TxReviewState(viewAction = null)
}

data class TxReviewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
