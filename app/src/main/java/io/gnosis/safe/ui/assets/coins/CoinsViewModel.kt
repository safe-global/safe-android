package io.gnosis.safe.ui.assets.coins

import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.assets.coins.CoinsViewData.Banner
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import kotlinx.coroutines.flow.collect
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import java.math.RoundingMode
import javax.inject.Inject

class CoinsViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
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
                val balanceInfo = tokenRepository.loadBalanceOf(safe, userDefaultFiat)

                val banner = when {
                    settingsHandler.showOwnerBanner && credentialsRepository.ownerCount() == 0 -> Banner.Type.ADD_OWNER_KEY
                    settingsHandler.showPasscodeBanner && credentialsRepository.ownerCount() > 0 -> Banner.Type.PASSCODE
                    else -> Banner.Type.NONE
                }
                val totalBalance = getTotalBalanceViewData(balanceInfo)
                val balances = getBalanceViewData(balanceInfo, banner)

                //TODO: [Send funds toggle] remove balancesWithFiat; use balances in update state
                val balancesWithTotal = mutableListOf<CoinsViewData>()
                balancesWithTotal.addAll(balances)
                if (balancesWithTotal[0] is Banner) {
                    balancesWithTotal.add(1, totalBalance)
                } else {
                    balancesWithTotal.add(0, totalBalance)
                }
                updateState { CoinsState(loading = false, refreshing = false, viewAction = UpdateBalances(totalBalance, balancesWithTotal)) }
            }
        }
    }

    fun isLoading(): Boolean {
        return (state.value as CoinsState).loading
    }

    suspend fun getTotalBalanceViewData(coinBalanceData: CoinBalances): CoinsViewData.TotalBalance {
        val userCurrencyCode = settingsHandler.userDefaultFiat

        return CoinsViewData.TotalBalance(
            balanceFormatter.fiatBalanceWithCurrency(
                coinBalanceData.fiatTotal.setScale(2, RoundingMode.HALF_UP),
                userCurrencyCode
            )
        )
    }

    suspend fun getBalanceViewData(coinBalanceData: CoinBalances, banner: Banner.Type): List<CoinsViewData> {
        val userCurrencyCode = settingsHandler.userDefaultFiat
        val result = mutableListOf<CoinsViewData>()

        when (banner) {
            Banner.Type.ADD_OWNER_KEY -> {
                result.add(Banner(Banner.Type.ADD_OWNER_KEY))
            }
            Banner.Type.PASSCODE -> {
                result.add(Banner(Banner.Type.PASSCODE))
            }
        }

        coinBalanceData.items.forEach {
            result.add(
                CoinsViewData.CoinBalance(
                    it.tokenInfo.address.asEthereumAddressChecksumString(),
                    it.tokenInfo.symbol,
                    it.tokenInfo.logoUri,
                    it.balance.convertAmount(it.tokenInfo.decimals),
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

    override fun onBannerDismissed(type: Banner.Type) {
        when (type) {
            Banner.Type.ADD_OWNER_KEY -> {
                settingsHandler.showOwnerBanner = false
                tracker.logBannerOwnerSkip()
            }
            Banner.Type.PASSCODE -> {
                settingsHandler.showPasscodeBanner = false
                tracker.logBannerPasscodeSkip()
            }
        }
        safeLaunch {
            updateState { CoinsState(loading = false, refreshing = false, viewAction = DismissOwnerBanner) }
        }
    }

    override fun onBannerActionTriggered(type: Banner.Type) {
        when (type) {
            Banner.Type.ADD_OWNER_KEY -> {
                settingsHandler.showOwnerBanner = false
                tracker.logBannerOwnerImport()
            }
            Banner.Type.PASSCODE -> {
                settingsHandler.showPasscodeBanner = false
                tracker.logBannerPasscodeCreate()
            }
        }
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
    val newTotalBalance: CoinsViewData.TotalBalance,
    val newBalances: List<CoinsViewData>
) : BaseStateViewModel.ViewAction

object DismissOwnerBanner: BaseStateViewModel.ViewAction
