package io.gnosis.safe.ui.settings.owner.selection

import androidx.paging.PagingSource
import com.keystone.module.MultiHDKeys
import pm.gnosis.model.Solidity
import timber.log.Timber

class KeystoneOwnerPagingSource(
    private val multiHDKeys: MultiHDKeys
) : PagingSource<Long, Solidity.Address>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Solidity.Address> {

        kotlin.runCatching {
            multiHDKeys.hdKeys.map {
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
