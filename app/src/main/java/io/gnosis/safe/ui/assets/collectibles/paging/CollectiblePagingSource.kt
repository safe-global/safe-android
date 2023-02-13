package io.gnosis.safe.ui.assets.collectibles.paging

import androidx.paging.PagingSource
import io.gnosis.data.models.Safe
import io.gnosis.data.models.assets.Collectible
import io.gnosis.data.repositories.TokenRepository
import timber.log.Timber

class CollectiblePagingSource(
    private val safe: Safe,
    private val tokenRepo: TokenRepository
) : PagingSource<String, Collectible>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Collectible> {

        val pageLink = params.key

        kotlin.runCatching {
            pageLink?.let { tokenRepo.loadCollectiblesPage(pageLink) }
                ?: tokenRepo.getCollectibles(safe)

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
