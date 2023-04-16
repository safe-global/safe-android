package io.gnosis.safe.ui.settings.owner.selection

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.keystone.module.MultiHDKeys
import io.gnosis.safe.utils.AddressPagingSource
import kotlinx.coroutines.flow.Flow
import pm.gnosis.model.Solidity

class KeystoneOwnerPagingProvider(
    private val multiHDKeys: MultiHDKeys
) {
    private val maxSize: Int
        get() = multiHDKeys.hdKeys.size

    fun getOwnersStream(): Flow<PagingData<Solidity.Address>> {
        return Pager(
            initialKey = 0,
            config = PagingConfig(
                pageSize = maxSize,
                prefetchDistance = 0,
                enablePlaceholders = true,
                initialLoadSize = maxSize,
                maxSize = maxSize
            ),
            pagingSourceFactory = {
                KeystoneOwnerPagingSource(multiHDKeys)
            }
        ).flow
    }
}
