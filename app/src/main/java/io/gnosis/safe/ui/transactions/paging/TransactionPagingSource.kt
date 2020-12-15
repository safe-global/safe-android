package io.gnosis.safe.ui.transactions.paging

import androidx.paging.PagingSource
import io.gnosis.data.models.transaction.TxListEntry
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.ui.transactions.TransactionListFragment
import pm.gnosis.model.Solidity
import timber.log.Timber

class TransactionPagingSource(
    private val safe: Solidity.Address,
    private val txRepo: TransactionRepository,
    private val type: TransactionListFragment.Type
) : PagingSource<String, TxListEntry>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, TxListEntry> {

        val pageLink = params.key

        kotlin.runCatching {
            pageLink?.let { txRepo.loadTransactionsPage(pageLink) }
                ?: if (type == TransactionListFragment.Type.HISTORY) txRepo.getHistoryTransactions(safe) else txRepo.getQueuedTransactions(safe)

        }.onSuccess {
            return LoadResult.Page(
                data = it.results,
                prevKey = it.previous,
                nextKey = it.next
            )
        }
            .onFailure {
                Timber.e(it)
                return LoadResult.Error(it)
            }

        throw IllegalStateException(javaClass.name)
    }
}
