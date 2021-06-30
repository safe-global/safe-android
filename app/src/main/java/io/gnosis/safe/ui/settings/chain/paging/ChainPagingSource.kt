package io.gnosis.safe.ui.settings.chain.paging

import androidx.paging.PagingSource
import io.gnosis.data.models.Chain
import io.gnosis.data.repositories.ChainInfoRepository
import timber.log.Timber

class ChainPagingSource(
    private val chainInfoRepository: ChainInfoRepository
) : PagingSource<String, Chain>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Chain> {

        val pageLink = params.key

        kotlin.runCatching {
            pageLink?.let { chainInfoRepository.loadChainInfoPage(pageLink) } ?: chainInfoRepository.getChainInfo()

        }.onSuccess { page ->
            return LoadResult.Page(
                data = page.results.map {
                    Chain(it.chainId, it.chainName, it.theme.textColor, it.theme.backgroundColor)
                },
                prevKey = page.previous,
                nextKey = page.next
            )
        }
            .onFailure {
                Timber.e(it)
                return LoadResult.Error(it)
            }

        throw IllegalStateException(javaClass.name)
    }
}
