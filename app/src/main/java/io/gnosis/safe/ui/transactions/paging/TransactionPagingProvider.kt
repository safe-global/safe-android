package io.gnosis.safe.ui.transactions.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.gnosis.data.models.transaction.UnifiedEntry
import io.gnosis.data.repositories.TransactionRepository
import kotlinx.coroutines.flow.Flow
import pm.gnosis.model.Solidity

class TransactionPagingProvider(
    private val transactionRepository: TransactionRepository
) {

    fun getTransactionsStream(safe: Solidity.Address): Flow<PagingData<UnifiedEntry>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { TransactionPagingSource(safe, transactionRepository) }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}
