package io.gnosis.safe.ui.assets.collectibles

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.assets.collectibles.paging.CollectiblePagingProvider
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.NoSafeSelected
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CollectiblesViewModel
@Inject constructor(
    private val collectiblesPager: CollectiblePagingProvider,
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<CollectiblesState>(appDispatchers) {

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { load() }
        }
    }

    override fun initialState(): CollectiblesState =
        CollectiblesState(loading = false, refreshing = false, viewAction = null)

    fun load(refreshing: Boolean = false) {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            if (safe != null) {
                if (!refreshing) {
                    updateState {
                        CollectiblesState(
                            loading = !refreshing,
                            refreshing = refreshing,
                            viewAction = if (refreshing) null else ViewAction.UpdateActiveSafe(safe)
                        )
                    }
                }
                getCollectibles(safe).collectLatest {
                    updateState {
                        CollectiblesState(
                            loading = false,
                            refreshing = false,
                            viewAction = UpdateCollectibles(it)
                        )
                    }
                }
            } else {
                updateState(forceViewAction = true) { CollectiblesState(loading = false, refreshing = false, viewAction = NoSafeSelected) }
            }
        }
    }

    fun isLoading(): Boolean {
        return (state.value as CollectiblesState).loading
    }

    private suspend fun getCollectibles(safe: Safe): Flow<PagingData<CollectibleViewData>> {
        val collectibleItems: Flow<PagingData<CollectibleViewData>> =
            collectiblesPager.getCollectiblesStream(safe)
                .map { pagingData ->
                    pagingData
                        .map {
                            CollectibleViewData.CollectibleItem(it, safe.chain)
                        }
                        .insertSeparators { before, after ->
                            when {
                                // the end of the list
                                after == null -> null
                                // first element
                                before == null ->
                                    CollectibleViewData.NftHeader(
                                        after.collectible.tokenName,
                                        after.collectible.logoUri,
                                        true
                                    )
                                before.collectible.address != after.collectible.address ->
                                    CollectibleViewData.NftHeader(
                                        after.collectible.tokenName,
                                        after.collectible.logoUri,
                                        false
                                    )
                                else -> null
                            }
                        }
                }
                .cachedIn(viewModelScope)

        return collectibleItems
    }
}

data class CollectiblesState(
    val loading: Boolean,
    val refreshing: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateCollectibles(
    val collectibles: PagingData<CollectibleViewData>
) : BaseStateViewModel.ViewAction
