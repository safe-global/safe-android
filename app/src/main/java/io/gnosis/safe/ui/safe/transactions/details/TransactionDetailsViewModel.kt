package io.gnosis.safe.ui.safe.transactions.details

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class TransactionDetailsViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionDetailsViewState>(appDispatchers) {

    override fun initialState() = TransactionDetailsViewState(null, true)
}

data class TransactionDetailsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State
