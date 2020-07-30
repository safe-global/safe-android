package io.gnosis.safe.ui.transactions.paging

import androidx.paging.PagingSource
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.Transaction
import io.gnosis.data.repositories.TransactionRepository
import pm.gnosis.model.Solidity


class TransactionPagingSource(
    private val safe: Solidity.Address,
    private val safeInfo: SafeInfo,
    private val txRepo: TransactionRepository
) : PagingSource<String, Transaction>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Transaction> {

        val pageLink = params.key

        kotlin.runCatching {
            pageLink?.let { txRepo.loadTransactionsPage(pageLink, safeInfo) } ?: txRepo.getTransactions(safe, safeInfo)

        }.onSuccess {
            return LoadResult.Page(
                data = it.results,
                prevKey = it.previous,
                nextKey = it.next
            )
        }
            .onFailure {
                return LoadResult.Error(it)

            }

        throw IllegalStateException(javaClass.name)
    }
}
