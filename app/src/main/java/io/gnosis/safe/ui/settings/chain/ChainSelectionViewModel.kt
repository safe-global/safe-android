package io.gnosis.safe.ui.settings.chain

import io.gnosis.data.models.Chain
import io.gnosis.data.repositories.ChainInfoRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class ChainSelectionViewModel
@Inject constructor(
    private val chainInfoRepository: ChainInfoRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ChainSelectionState>(appDispatchers) {

    override fun initialState() = ChainSelectionState(ViewAction.Loading(true))

    fun loadChains() {
        safeLaunch {

            updateState {
                ChainSelectionState(ViewAction.Loading(true))
            }

            val chains = chainInfoRepository.getChains()

            updateState {
                ChainSelectionState(ShowChains(chains))
            }
        }
    }
}

data class ShowChains(
    val chains: List<Chain>
) : BaseStateViewModel.ViewAction

data class ChainSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
