package io.gnosis.safe.ui.assets.coins

import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.OwnerCredentialsRepository
import io.gnosis.safe.utils.convertAmount
import kotlinx.coroutines.flow.collect
import java.math.RoundingMode
import javax.inject.Inject

class CoinsViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    private val settingsHandler: SettingsHandler,
    private val balanceFormatter: BalanceFormatter,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<CoinsState>(appDispatchers),
    CoinsAdapter.OwnerBannerListener {

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
                val showBanner = settingsHandler.showOwnerBanner && !ownerCredentialsRepository.hasCredentials()
                val balances = getBalanceViewData(balanceInfo, showBanner)
                updateState { CoinsState(loading = false, refreshing = false, viewAction = UpdateBalances(balances)) }
            }
        }
    }

    suspend fun getBalanceViewData(coinBalanceData: CoinBalances, showBanner: Boolean): List<CoinsViewData> {
        val userCurrencyCode = settingsHandler.userDefaultFiat
        val result = mutableListOf<CoinsViewData>()

        if (showBanner) {
            result.add(CoinsViewData.Banner)
        }

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

    override fun onBannerDismissed() {
        settingsHandler.showOwnerBanner = false
        tracker.logBannerOwnerSkipped()
        safeLaunch {
            updateState { CoinsState(loading = false, refreshing = false, viewAction = DismissOwnerBanner) }
        }
    }

    override fun onBannerActionTriggered() {
        settingsHandler.showOwnerBanner = false
        tracker.logBannerOwnerImport()
        safeLaunch {
            updateState { CoinsState(loading = false, refreshing = false, viewAction = DismissOwnerBanner) }
        }
    }
}

data class CoinsState(
    val loading: Boolean,
    val refreshing: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateBalances(
    val newBalances: List<CoinsViewData>
) : BaseStateViewModel.ViewAction

object DismissOwnerBanner: BaseStateViewModel.ViewAction
