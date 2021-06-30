package io.gnosis.safe.ui.settings.chain

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.gnosis.data.models.Chain
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.chain.paging.ChainPagingProvider
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

class ChainSelectionViewModel
@Inject constructor(
    private val chainPager: ChainPagingProvider,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ChainSelectionState>(appDispatchers) {

    override fun initialState() = ChainSelectionState(ViewAction.Loading(true))

    fun loadChains() {
        safeLaunch {

            updateState {
                ChainSelectionState(ViewAction.Loading(true))
            }

            chainPager.getChainsStream().cachedIn(viewModelScope).collectLatest {
                updateState {
                    ChainSelectionState(ShowChains(it))
                }
            }
        }
    }
}

data class ShowChains(
    val chains: PagingData<Chain>
) : BaseStateViewModel.ViewAction

data class ChainSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
