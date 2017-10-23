package pm.gnosis.heimdall.utils

import android.support.v7.util.DiffUtil
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.DataResult
import pm.gnosis.heimdall.common.util.ErrorResult
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.ui.base.Adapter


fun <D> Flowable<List<D>>.scanToAdapterData(itemCheck: ((D, D) -> Boolean), contentCheck: ((D, D) -> Boolean)? = null): Flowable<Adapter.Data<D>> =
        scan(Adapter.Data(), scanner(itemCheck, contentCheck))

fun <D> Observable<List<D>>.scanToAdapterData(itemCheck: ((D, D) -> Boolean), contentCheck: ((D, D) -> Boolean)? = null): Observable<Adapter.Data<D>> =
        scan(Adapter.Data(), scanner(itemCheck, contentCheck))

fun <D> Observable<Result<List<D>>>.scanToAdapterDataResult(itemCheck: ((D, D) -> Boolean), contentCheck: ((D, D) -> Boolean)? = null): Observable<out Result<Adapter.Data<D>>> =
        scan<CachedScanResult<D>>(CachedScanResult.empty(), { old, new ->
            when (new) {
                is ErrorResult -> old.copy(result = ErrorResult(new.error))
                is DataResult -> CachedScanResult.withData(scanner(itemCheck, contentCheck)(old.data, new.data))
            }
        }).map { it.result }

fun <D> scanner(itemCheck: (D, D) -> Boolean, contentCheck: ((D, D) -> Boolean)?): (Adapter.Data<D>, List<D>) -> Adapter.Data<D> {
    return { data, newEntries ->
        val diff = DiffUtil.calculateDiff(SimpleDiffCallback(data.entries, newEntries, itemCheck, contentCheck))
        Adapter.Data(data.id, newEntries, diff)
    }
}

private data class CachedScanResult<out D>(val data: Adapter.Data<D>, val result: Result<Adapter.Data<D>>) {
    companion object {
        fun <D> empty() = withData(Adapter.Data<D>())
        fun <D> withData(data: Adapter.Data<D>) = CachedScanResult(data, DataResult(data))
    }
}

private class SimpleDiffCallback<D>(
        private val prevEntries: List<D>, private val newEntries: List<D>,
        private val itemCheck: ((D, D) -> Boolean), private val contentCheck: ((D, D) -> Boolean)?
) : DiffUtil.Callback() {
    override fun getOldListSize() = prevEntries.size

    override fun getNewListSize() = newEntries.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val prevEntry = prevEntries.getOrNull(oldItemPosition) ?: return false
        val newEntry = newEntries.getOrNull(newItemPosition) ?: return false
        return itemCheck(prevEntry, newEntry)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        contentCheck ?: return false
        val prevEntry = prevEntries.getOrNull(oldItemPosition) ?: return false
        val newEntry = newEntries.getOrNull(newItemPosition) ?: return false
        return contentCheck.invoke(prevEntry, newEntry)
    }
}
