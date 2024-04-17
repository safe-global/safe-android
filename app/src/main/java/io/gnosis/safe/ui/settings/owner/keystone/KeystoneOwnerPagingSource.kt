package io.gnosis.safe.ui.settings.owner.keystone

import androidx.paging.PagingSource
import com.keystone.module.MultiAccounts
import pm.gnosis.model.Solidity
import timber.log.Timber

class KeystoneOwnerPagingSource(
    private val multiHDKeys: MultiAccounts
) : PagingSource<Long, Solidity.Address>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Solidity.Address> {

        kotlin.runCatching {
            multiHDKeys.keys.map {
                it.toAddress()
            }
        }.onSuccess {
            return LoadResult.Page(
                data = it,
                prevKey = null,
                nextKey = null
            )
        }
            .onFailure {
                Timber.e(it)
                return LoadResult.Error(it)
            }

        throw IllegalStateException(javaClass.name)
    }
}
