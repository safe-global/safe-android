package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.data.models.Chain
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.UnstoppableDomainsRepository
import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigDecimal
import javax.inject.Inject

class SendAssetViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val unstoppableDomainsRepository: UnstoppableDomainsRepository,
    private val ensRepository: EnsRepository,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SendAssetState>(appDispatchers) {

    lateinit var activeSafe: Safe
        private set

    override fun initialState(): SendAssetState =
        SendAssetState(viewAction = null)

    init {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()!!
        }
    }

    fun onReviewButtonClicked(
        chain: Chain,
        asset: CoinsViewData.CoinBalance,
        toAddress: String,
        amount: BigDecimal
    ) {
        safeLaunch {
            updateState {
                SendAssetState(
                    viewAction = ViewAction.NavigateTo(
                        SendAssetFragmentDirections.actionSendAssetFragmentToSendAssetReviewFragment(
                            chain,
                            asset,
                            activeSafe.address.asEthereumAddressString(),
                            toAddress,
                            amount.toPlainString()
                        )
                    )
                )
            }
            updateState {
                SendAssetState(
                    viewAction = ViewAction.None
                )
            }
        }
    }

    fun enableUD(chain: Chain): Boolean {
        return unstoppableDomainsRepository.canResolve(chain)
    }

    fun enableENS(chain: Chain): Boolean {
        return ensRepository.canResolve(chain)
    }

    fun isChainPrefixPrependEnabled(): Boolean = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled(): Boolean = settingsHandler.chainPrefixCopy
}

data class SendAssetState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
