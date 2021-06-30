package io.gnosis.safe.ui.transactions.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.TxListEntry
import io.gnosis.data.repositories.TransactionRepository
import kotlinx.coroutines.flow.Flow

class TransactionPagingProvider(
    private val transactionRepository: TransactionRepository
) {

    fun getTransactionsStream(safe: Safe, type: TransactionPagingSource.Type): Flow<PagingData<TxListEntry>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { TransactionPagingSource(safe, transactionRepository, type) }
        ).flow
    }

    override fun toString(): String {
        return "TransactionPagingProvider(transactionRepository=$transactionRepository)"
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}
