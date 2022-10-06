package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import java.math.RoundingMode
import javax.inject.Inject

class AssetSelectionViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    private val settingsHandler: SettingsHandler,
    private val balanceFormatter: BalanceFormatter,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<AssetSelectionState>(appDispatchers) {

    override fun initialState(): AssetSelectionState = AssetSelectionState(loading = false, refreshing = false, viewAction = null)

    fun load() {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            val userDefaultFiat = settingsHandler.userDefaultFiat
            if (safe != null) {
                updateState {
                    AssetSelectionState(
                        loading = !refreshing,
                        refreshing = refreshing,
                        viewAction = null
                    )
                }
                val balanceInfo = tokenRepository.loadBalanceOf(safe, userDefaultFiat)
                val balances = getBalanceViewData(balanceInfo)
                updateState { AssetSelectionState(loading = false, refreshing = false, viewAction = UpdateAssetSelection(null, balances)) }
            }
        }
    }

    fun selectAssetForTransfer(assetData: CoinsViewData.CoinBalance) {
        //TODO: select asset
    }

    suspend fun getBalanceViewData(coinBalanceData: CoinBalances): List<CoinsViewData> {
        val userCurrencyCode = settingsHandler.userDefaultFiat
        val result = mutableListOf<CoinsViewData>()

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

data class AssetSelectionState(
    val loading: Boolean,
    val refreshing: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateAssetSelection(
    val selectedAsset: CoinsViewData.CoinBalance?,
    val balances: List<CoinsViewData>,
) :BaseStateViewModel.ViewAction
