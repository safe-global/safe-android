package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.data.models.assets.Balance
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.ui.assets.coins.CoinsAdapter
import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import java.math.RoundingMode
import javax.inject.Inject

class AssetSelectionViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    private val settingsHandler: SettingsHandler,
    private val balanceFormatter: BalanceFormatter,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<AssetSelectionState>(appDispatchers), CoinsAdapter.AssetOnClickListener {

    override fun initialState(): AssetSelectionState =
        AssetSelectionState(loading = false, viewAction = null)

    fun load(filterTerm: String = "") {
        val term = filterTerm.toLowerCase()
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            val userDefaultFiat = settingsHandler.userDefaultFiat
            if (safe != null) {
                updateState {
                    AssetSelectionState(
                        loading = true,
                        viewAction = null
                    )
                }
                val balances = tokenRepository.loadBalanceOf(safe, userDefaultFiat)
                    .items
                    .filter {
                        if (filterTerm.isNotBlank()) {
                            with(it.tokenInfo) {
                                symbol.toLowerCase().contains(term) ||
                                        name.toLowerCase().contains(term)
                            }
                        } else {
                            true
                        }
                    }

                if (balances.isNotEmpty()) {
                    val balancesViewData = getBalanceViewData(balances)
                    updateState {
                        AssetSelectionState(
                            loading = false,
                            viewAction = UpdateAssetSelection(null, balancesViewData)
                        )
                    }
                } else {
                    updateState {
                        AssetSelectionState(
                            loading = false,
                            viewAction = ViewAction.ShowEmptyState
                        )
                    }
                }
            }
        }
    }

    fun isLoading(): Boolean {
        return (state.value as AssetSelectionState).loading
    }

    suspend fun getBalanceViewData(balances: List<Balance>): List<CoinsViewData> {
        val userCurrencyCode = settingsHandler.userDefaultFiat
        val result = mutableListOf<CoinsViewData>()

        balances.forEach {
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

    override fun onAssetClicked(asset: CoinsViewData.CoinBalance) {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()!!
            updateState {
                //TODO: pass selected asset and proceed with the flow
                AssetSelectionState(
                    loading = false,
                    //viewAction = ViewAction.NavigateTo(AssetSelectionFragmentDirections.actionAssetSelectionFragmentToSuccessFragment(safe.chain, "", "22", "SAFE"))
                   // viewAction = ViewAction.NavigateTo(AssetSelectionFragmentDirections.actionAssetSelectionFragmentToEditAdvancedParamsFragment(safe.chain, "5", "1"))
                    viewAction = ViewAction.NavigateTo(AssetSelectionFragmentDirections.actionAssetSelectionFragmentToSendAssetFragment(safe.chain, asset))
                )
            }
            updateState {
                AssetSelectionState(
                    loading = false,
                    viewAction = ViewAction.None
                )
            }
        }
    }
}

data class AssetSelectionState(
    val loading: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateAssetSelection(
    val selectedAsset: CoinsViewData.CoinBalance?,
    val balances: List<CoinsViewData>,
) : BaseStateViewModel.ViewAction
