package io.gnosis.safe.ui.safe.balances.coins

import io.gnosis.data.models.Balance
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class CoinsViewModel
@Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<CoinsState>(appDispatchers) {

    private val tokenRepositories = repositories.tokenRepository()
    private val safeRepository = repositories.safeRepository()

    override fun initialState(): CoinsState = CoinsState(false, null)

    fun loadFor(isRefresh: Boolean = false) {
        safeLaunch {
            updateState { CoinsState(true, null) }
            val balances = tokenRepositories.loadBalanceOfNew(safeRepository.getActiveSafe()!!.address)
            updateState { CoinsState(false, UpdateBalances(balances)) }
        }
    }
}

data class CoinsState(
    val isLoading: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateBalances(
    val newBalances: List<Balance>
) : BaseStateViewModel.ViewAction
