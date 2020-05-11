package io.gnosis.safe.ui.safe.balances.coins

import io.gnosis.data.models.Balance
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class CoinsViewModel
@Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<CoinsState>(appDispatchers) {

    private val tokenRepositories = repositories.tokenRepository()
    private val safeRepository = repositories.safeRepository()

    override fun initialState(): CoinsState = CoinsState(loading = false, refreshing = false, viewAction = null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { load() }
        }
    }

    fun load(isRefresh: Boolean = false) {
        safeLaunch {
            updateState { CoinsState(loading = !isRefresh, refreshing = isRefresh, viewAction = null) }
            val balances = tokenRepositories.loadBalanceOfNew(safeRepository.getActiveSafe()!!.address)
            updateState { CoinsState(loading = false, refreshing = false, viewAction = UpdateBalances(balances)) }
        }
    }
}

data class CoinsState(
    val loading: Boolean,
    val refreshing: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateBalances(
    val newBalances: List<Balance>
) : BaseStateViewModel.ViewAction
