package pm.gnosis.heimdall.ui.safe.details.transactions

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.mapScanToMappedResult
import java.math.BigInteger
import javax.inject.Inject


class SafeTransactionsViewModel @Inject constructor(
        private val safeRepository: GnosisSafeRepository
) : SafeTransactionsContract() {

    companion object {
        const val PAGE_SIZE = 8
    }

    private var loadingMore: Boolean = false
    private var cachedResults: IndexedResults? = null
    private var address: BigInteger? = null

    override fun setup(address: BigInteger) {
        if (this.address != address) {
            cachedResults = null
        }
        this.address = address
    }

    override fun initTransaction(reload: Boolean): Single<Result<Int>> {
        if (reload) {
            cachedResults = null
        }
        return (cachedResults?.let { Single.just(it) } ?:
                safeRepository.loadDescriptionCount(address!!)
                        .flatMap(this::loadDescription)
                        .doOnSuccess { cachedResults = it })
                .map({ it.data.entries.size })
                .mapToResult()
    }

    override fun observeTransaction(loadMoreEvents: Observable<Unit>): Observable<out Result<PaginatedTransactions>> {
        val initialResults = mapResults(initialData())
        return loadMoreEvents
                .filter { !loadingMore }
                .flatMapMaybe { moreTransactions().doOnSubscribe { loadingMore = true }.doAfterTerminate { loadingMore = false } }
                .mapScanToMappedResult(
                        { previous: Adapter.Data<String>, new -> previous.entries + new.data.entries },
                        { new, data ->
                            cachedResults = IndexedResults(cachedResults?.startIndex ?: new.startIndex, new.endIndex, data)
                            mapResults(new, data)
                        },
                        initialResults.data, initialResults
                )
    }

    private fun mapResults(results: IndexedResults, data: Adapter.Data<String> = results.data)
            = PaginatedTransactions(results.endIndex > 0, data)

    private fun initialData() = cachedResults ?: IndexedResults(-1, -1, Adapter.Data())

    private fun moreTransactions(): Maybe<Result<IndexedResults>> {
        val startIndex = (cachedResults?.endIndex ?: 0)
        if (startIndex < 0) {
            return Maybe.empty()
        }
        return loadDescription(startIndex)
                .mapToResult()
                .toMaybe()
    }

    private fun loadDescription(startIndex: Int): Single<IndexedResults> {
        if (startIndex == 0) {
            return Single.just(IndexedResults(0, 0, Adapter.Data()))
        }
        val endIndex = Math.max(0, startIndex - PAGE_SIZE)
        // We load reversed, because we want the oldest first
        return safeRepository.loadDescriptions(address!!, endIndex, startIndex)
                .map { IndexedResults(startIndex, endIndex, Adapter.Data(entries = it.reversed())) }
    }

    private data class IndexedResults(val startIndex: Int, val endIndex: Int, val data: Adapter.Data<String>)

}