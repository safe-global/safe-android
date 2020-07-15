package io.gnosis.safe.ui.safe.transactions.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.Transaction
import io.gnosis.data.repositories.TransactionRepository
import kotlinx.coroutines.flow.Flow
import pm.gnosis.model.Solidity

class TransactionPagingProvider(
    private val transactionRepository: TransactionRepository) {

    fun getTransactionsStream(safe: Solidity.Address, safeInfo: SafeInfo): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { TransactionPagingSource(safe, safeInfo, transactionRepository) }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}
