package io.gnosis.safe.ui.settings.owner.selection

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.gnosis.safe.utils.AddressPagingSource
import kotlinx.coroutines.flow.Flow
import pm.gnosis.model.Solidity

class DerivedOwnerPagingProvider(
    private val derivator: AddressPagingSource
) {

    fun getOwnersStream(): Flow<PagingData<Solidity.Address>> {
        return Pager(
            initialKey = 0,
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = 1,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE,
                maxSize = PAGE_SIZE * MAX_PAGES
            ),
            pagingSourceFactory = {
                DerivedOwnerPagingSource(derivator, MAX_PAGES)
            }
        ).flow
    }

    companion object {
        const val PAGE_SIZE = 20
        const val MAX_PAGES = 5
    }
}
