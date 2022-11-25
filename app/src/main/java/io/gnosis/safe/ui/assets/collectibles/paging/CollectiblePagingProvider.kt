package io.gnosis.safe.ui.assets.collectibles.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.gnosis.data.models.Safe
import io.gnosis.data.models.assets.Collectible
import io.gnosis.data.repositories.TokenRepository
import kotlinx.coroutines.flow.Flow

class CollectiblePagingProvider(
    private val tokenRepository: TokenRepository
) {

    fun getCollectiblesStream(safe: Safe): Flow<PagingData<Collectible>> {
        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { CollectiblePagingSource(safe, tokenRepository) }
        ).flow
    }

    override fun toString(): String {
        return "CollectiblePagingProvider(tokenRepository=$tokenRepository)"
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}
