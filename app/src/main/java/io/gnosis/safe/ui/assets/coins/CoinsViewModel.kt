package io.gnosis.safe.ui.assets.coins

import android.net.ConnectivityManager
import io.gnosis.data.models.Balance
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class CoinsViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers,
    connectivityManager: ConnectivityManager? = null
) : BaseStateViewModel<CoinsState>(appDispatchers, connectivityManager) {

    override fun initialState(): CoinsState = CoinsState(loading = false, refreshing = false, viewAction = null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { load() }
        }
    }

    fun load(refreshing: Boolean = false) {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            if (safe != null) {
                updateState { CoinsState(loading = !refreshing, refreshing = refreshing, viewAction = null) }
                val balances = tokenRepository.loadBalanceOf(safe.address)
                updateState { CoinsState(loading = false, refreshing = false, viewAction = UpdateBalances(balances)) }
            }
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
