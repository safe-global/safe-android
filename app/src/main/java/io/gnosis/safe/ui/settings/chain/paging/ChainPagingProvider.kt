package io.gnosis.safe.ui.settings.chain.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.gnosis.data.models.Chain
import io.gnosis.data.repositories.ChainInfoRepository
import kotlinx.coroutines.flow.Flow

class ChainPagingProvider(
    private val chainInfoRepository: ChainInfoRepository
) {

    fun getChainsStream(): Flow<PagingData<Chain>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { ChainPagingSource(chainInfoRepository) }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}
