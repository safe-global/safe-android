package io.gnosis.safe.ui.assets.coins

import io.gnosis.data.models.CoinBalances
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import kotlinx.coroutines.flow.collect
import java.math.RoundingMode
import javax.inject.Inject

class CoinsViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    private val balanceFormatter: BalanceFormatter,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<CoinsState>(appDispatchers) {

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
                updateState {
                    CoinsState(
                        loading = !refreshing,
                        refreshing = refreshing,
                        viewAction = if (refreshing) null else ViewAction.UpdateActiveSafe(safe)
                    )
                }
                val balanceInfo = tokenRepository.loadBalanceOf(safe.address)
                val balances = getBalanceViewData(balanceInfo)

                updateState { CoinsState(loading = false, refreshing = false, viewAction = UpdateBalances(Adapter.Data(null, balances))) }
            }
        }
    }

    fun getBalanceViewData(coinBalanceData: CoinBalances): List<CoinsViewData> {

        val result = mutableListOf<CoinsViewData>()

        val totalBalance = CoinsViewData.TotalBalance(
            "$ ${balanceFormatter.shortAmount(coinBalanceData.fiatTotal.setScale(2, RoundingMode.HALF_UP))}"
        )
        result.add(totalBalance)

        coinBalanceData.items.forEach {
            result.add(CoinsViewData.CoinBalance(
                it.tokenInfo.symbol,
                it.tokenInfo.logoUri,
                balanceFormatter.shortAmount(it.balance.convertAmount(it.tokenInfo.decimals)),
                "$ ${balanceFormatter.shortAmount(it.fiatBalance.setScale(2, RoundingMode.HALF_UP))}"

            ))
        }

        return result
    }
}

data class CoinsState(
    val loading: Boolean,
    val refreshing: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateBalances(
    val newBalances: Adapter.Data<CoinsViewData>
) : BaseStateViewModel.ViewAction
