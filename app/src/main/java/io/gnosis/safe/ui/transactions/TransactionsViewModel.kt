package io.gnosis.safe.ui.transactions

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class TransactionsViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionsState>(appDispatchers) {

    override fun initialState() = TransactionsState(null, ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    TransactionsState(safe, null)
                }
            }
        }
    }
}

data class TransactionsState(
    val safe: Safe?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
