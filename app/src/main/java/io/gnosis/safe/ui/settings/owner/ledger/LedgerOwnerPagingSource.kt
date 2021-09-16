package io.gnosis.safe.ui.settings.owner.ledger

import androidx.paging.PagingSource
import pm.gnosis.model.Solidity
import timber.log.Timber

class LedgerOwnerPagingSource(
    private val ledgerController: LedgerController,
    private val derivationPath: String,
    private val maxPages: Int
) : PagingSource<Long, Solidity.Address>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Solidity.Address> {
        val pageLink = params.key
        val pageSize = params.loadSize

        kotlin.runCatching {

            pageLink?.let { ledgerController.addressesForPage(derivationPath, pageLink, pageSize) } ?: ledgerController.addressesForPage(
                derivationPath,
                0,
                pageSize
            )

        }.onSuccess {
            return LoadResult.Page(
                data = it,
                prevKey = if (pageLink == null || pageLink == 0L) null else pageLink - pageSize,
                nextKey = if ((pageLink ?: 0) < (maxPages - 1) * pageSize) (pageLink ?: 0) + pageSize else null
            )
        }
            .onFailure {
                Timber.e(it)
                return LoadResult.Error(it)
            }

        throw IllegalStateException(javaClass.name)
    }
}
