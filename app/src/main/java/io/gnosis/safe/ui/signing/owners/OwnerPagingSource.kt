package io.gnosis.safe.ui.signing.owners

import androidx.paging.PagingSource
import io.gnosis.safe.utils.MnemonicAddressDerivator
import pm.gnosis.model.Solidity
import timber.log.Timber


class OwnerPagingSource(
    private val derivator: MnemonicAddressDerivator,
    private val numPages: Int
) : PagingSource<Long, Solidity.Address>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Solidity.Address> {

        val pageLink = params.key

        kotlin.runCatching {
            pageLink?.let { derivator.addressesForPage(pageLink, 20) } ?: derivator.addressesForPage(0, 20)

        }.onSuccess {
            return LoadResult.Page(
                data = it,
                prevKey =  if (pageLink == null || pageLink == 0L) null else pageLink - (20),
                nextKey = if ((pageLink ?: 0) < (numPages - 1) * 20) (pageLink ?: 0)  + 20 else null
            )
        }
            .onFailure {
                Timber.e(it)
                return LoadResult.Error(it)
            }

        throw IllegalStateException(javaClass.name)
    }
}
