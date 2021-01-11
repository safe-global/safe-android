package io.gnosis.safe.ui.assets.coins

import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import kotlinx.coroutines.flow.collect
import java.math.RoundingMode
import java.util.*
import javax.inject.Inject

class CoinsViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    private val settingsHandler: SettingsHandler,
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
            val userDefaultFiat = settingsHandler.userDefaultFiat
            if (safe != null) {
                updateState {
                    CoinsState(
                        loading = !refreshing,
                        refreshing = refreshing,
                        viewAction = if (refreshing) null else ViewAction.UpdateActiveSafe(safe)
                    )
                }
                val balanceInfo = tokenRepository.loadBalanceOf(safe.address, userDefaultFiat)
                val balances = getBalanceViewData(balanceInfo)

                updateState { CoinsState(loading = false, refreshing = false, viewAction = UpdateBalances(Adapter.Data(null, balances))) }
            }
        }
    }

    suspend fun getBalanceViewData(coinBalanceData: CoinBalances): List<CoinsViewData> {
        val userCurrencyCode = settingsHandler.userDefaultFiat
        val result = mutableListOf<CoinsViewData>()

        val totalBalance = CoinsViewData.TotalBalance(
            balanceFormatter.fiatBalanceWithCurrency(
                coinBalanceData.fiatTotal.setScale(2, RoundingMode.HALF_UP),
                userCurrencyCode
            )
        )
        result.add(totalBalance)

        coinBalanceData.items.forEach {
            result.add(
                CoinsViewData.CoinBalance(
                    it.tokenInfo.symbol,
                    it.tokenInfo.logoUri,
                    balanceFormatter.shortAmount(it.balance.convertAmount(it.tokenInfo.decimals)),
                    balanceFormatter.fiatBalanceWithCurrency(
                        it.fiatBalance.setScale(2, RoundingMode.HALF_UP),
                        userCurrencyCode
                    )
                )
            )
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
