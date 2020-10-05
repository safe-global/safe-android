package io.gnosis.safe.ui.signing.owners

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.gnosis.safe.utils.MnemonicAddressDerivator
import kotlinx.coroutines.flow.Flow
import pm.gnosis.model.Solidity

class OwnerPagingProvider(
    private val derivator: MnemonicAddressDerivator
) {

    fun getOwnersStream(): Flow<PagingData<Solidity.Address>> {
        return Pager(
            initialKey = 0,
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = 1,
                enablePlaceholders = false,
                initialLoadSize = 0,
                maxSize = PAGE_SIZE * PAGES_THRESHOLD
            ),
            pagingSourceFactory = {
                OwnerPagingSource(
                    derivator,
                    PAGES_THRESHOLD
                )
            }
        ).flow
    }

    companion object {
        const val PAGE_SIZE = 20
        const val PAGES_THRESHOLD = 5
    }
}
