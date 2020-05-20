package io.gnosis.safe.ui.transaction

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.models.Transaction
import javax.inject.Inject

class TransactionsViewModel
@Inject constructor(appDispatchers: AppDispatchers) : BaseStateViewModel<TransactionsViewState>(appDispatchers) {

    override fun initialState(): TransactionsViewState = TransactionsViewState(null, false)
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: List<Transaction>
) : BaseStateViewModel.ViewAction
