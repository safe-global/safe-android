package io.gnosis.safe.ui.assets.collectibles

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.adapter.Adapter
import pm.gnosis.model.Solidity
import javax.inject.Inject

class CollectiblesViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<CollectiblesState>(appDispatchers) {

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
                        viewAction = UpdateCollectibles(
                            Adapter.Data(null, collectibles)
                        )
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
                var contractLogoUri: String? = null
                kotlin.runCatching {
                    tokenRepository.loadTokenInfo(it.address)
                }
                    .onSuccess {
                        contractLogoUri = it.logoUri
                    }

                collectiblesViewData.add(
                    CollectibleViewData.NftHeader(
                        it.tokenName,
                        contractLogoUri
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
