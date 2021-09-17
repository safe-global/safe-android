package io.gnosis.safe.ui.settings.owner.ledger

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pm.gnosis.model.Solidity

class LedgerOwnerPagingProvider(
    private val ledgerController: LedgerController,
    private val derivationPath: String
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
                LedgerOwnerPagingSource(ledgerController, derivationPath, MAX_PAGES)
            }
        ).flow
    }

    companion object {
        const val PAGE_SIZE = 5
        const val MAX_PAGES = 20
    }
}
