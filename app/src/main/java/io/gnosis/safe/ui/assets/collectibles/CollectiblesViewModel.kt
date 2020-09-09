package io.gnosis.safe.ui.assets.collectibles

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.adapter.Adapter
import kotlinx.coroutines.flow.collect
import pm.gnosis.model.Solidity
import javax.inject.Inject

class CollectiblesViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<CollectiblesState>(appDispatchers) {

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { load() }
        }
    }

    override fun initialState(): CollectiblesState = CollectiblesState(loading = false, refreshing = false, viewAction = null)

    fun load(refreshing: Boolean = false) {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            if (safe != null) {
                updateState {
                    CollectiblesState(
                        loading = !refreshing,
                        refreshing = refreshing,
                        viewAction = if (refreshing) null else ViewAction.UpdateActiveSafe(safe)
                    )
                }
                val collectibles = getCollectibles(safe.address)
                updateState {
                    CollectiblesState(
                        loading = false,
                        refreshing = false,
                        viewAction = if (collectibles.isEmpty()) ViewAction.ShowEmptyState else UpdateCollectibles(Adapter.Data(null, collectibles))
                    )
                }
            }
        }
    }

    private suspend fun getCollectibles(safe: Solidity.Address): List<CollectibleViewData> {

        val collectiblesViewData = mutableListOf<CollectibleViewData>()
        val collectibles = tokenRepository.loadCollectiblesOf(safe)

        var currentNft: Solidity.Address? = null

        collectibles.forEach {

            if (currentNft != it.address) {
                collectiblesViewData.add(
                    CollectibleViewData.NftHeader(
                        it.tokenName,
                        it.logoUri,
                        collectiblesViewData.isEmpty()
                    )
                )
                currentNft = it.address
            }

            collectiblesViewData.add(
                CollectibleViewData.CollectibleItem(it)
            )
        }

        return collectiblesViewData
    }
}

data class CollectiblesState(
    val loading: Boolean,
    val refreshing: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateCollectibles(
    val collectibles: Adapter.Data<CollectibleViewData>
) : BaseStateViewModel.ViewAction
