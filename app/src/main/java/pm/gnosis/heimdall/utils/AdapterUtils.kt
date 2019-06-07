package pm.gnosis.heimdall.utils

import androidx.recyclerview.widget.DiffUtil
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result

fun <D> Flowable<List<D>>.scanToAdapterData(
    idExtractor: ((D) -> Any) = defaultIdExtractor(),
    payloadCalc: ((D, D) -> Any?)? = null,
    initialData: Adapter.Data<D>? = null
): Flowable<Adapter.Data<D>> =
    scan(initialData ?: Adapter.Data(), scanner(idExtractor, payloadCalc))

fun <D> Observable<List<D>>.scanToAdapterData(
    idExtractor: ((D) -> Any) = defaultIdExtractor(),
    payloadCalc: ((D, D) -> Any?)? = null,
    initialData: Adapter.Data<D>? = null
): Observable<Adapter.Data<D>> =
    scan(initialData ?: Adapter.Data(), scanner(idExtractor, payloadCalc))

fun <D> Observable<Result<List<D>>>.scanToAdapterDataResult(
    idExtractor: ((D) -> Any) = defaultIdExtractor(),
    payloadCalc: ((D, D) -> Any?)? = null
): Observable<out Result<Adapter.Data<D>>> {
    val initialData = Adapter.Data<D>()
    return mapScanToAdapterDataResult({ _, i -> i }, initialData, idExtractor, payloadCalc)
}

fun <D> Observable<Result<List<D>>>.mapScanToAdapterDataResult(
    inMapper: ((Adapter.Data<D>, List<D>) -> List<D>), initialData: Adapter.Data<D>,
    idExtractor: ((D) -> Any) = defaultIdExtractor(), payloadCalc: ((D, D) -> Any?)? = null
): Observable<out Result<Adapter.Data<D>>> {
    return mapScanToMappedResult(inMapper, { _, o -> o }, initialData, initialData, idExtractor, payloadCalc)
}

fun <I, O, D> Observable<Result<I>>.mapScanToMappedResult(
    inMapper: ((Adapter.Data<D>, I) -> List<D>), outMapper: ((I, Adapter.Data<D>) -> O),
    initialData: Adapter.Data<D>, initialOutput: O,
    idExtractor: ((D) -> Any) = defaultIdExtractor(), payloadCalc: ((D, D) -> Any?)? = null
): Observable<out Result<O>> =
    scan<CachedScanResult<O, D>>(
        CachedScanResult(initialData, DataResult(initialOutput)),
        { old, new ->
            when (new) {
                is ErrorResult -> old.copy(result = ErrorResult(new.error))
                is DataResult -> {
                    calculateCachedScanResults(inMapper, outMapper, idExtractor, payloadCalc, old.data, new.data)
                }
            }
        }).map { it.result }

private fun <D> defaultIdExtractor(): (D) -> Any = { entry -> entry as Any }

private fun <I, O, D> calculateCachedScanResults(
    inMapper: ((Adapter.Data<D>, I) -> List<D>), outMapper: ((I, Adapter.Data<D>) -> O),
    idExtractor: ((D) -> Any), payloadCalc: ((D, D) -> Any?)?,
    cachedData: Adapter.Data<D>, input: I
): CachedScanResult<O, D> {
    val mappedData = inMapper(cachedData, input)
    val adapterData = scanner(idExtractor, payloadCalc)(cachedData, mappedData)
    return CachedScanResult(adapterData, DataResult(outMapper(input, adapterData)))
}

private fun <D> scanner(idExtractor: ((D) -> Any), payloadCalc: ((D, D) -> Any?)?): (Adapter.Data<D>, List<D>) -> Adapter.Data<D> {
    return { data, newEntries ->
        val diff = DiffUtil.calculateDiff(SimpleDiffCallback(data.entries, newEntries, idExtractor, payloadCalc))
        Adapter.Data(data.id, newEntries, diff)
    }
}

private data class CachedScanResult<out W, out D>(val data: Adapter.Data<D>, val result: Result<W>)

private class SimpleDiffCallback<D>(
    private val prevEntries: List<D>, private val newEntries: List<D>,
    private val idExtractor: ((D) -> Any), private val payloadCalc: ((D, D) -> Any?)?
) : DiffUtil.Callback() {
    override fun getOldListSize() = prevEntries.size

    override fun getNewListSize() = newEntries.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val prevId = prevEntries.getOrNull(oldItemPosition)?.let { idExtractor(it) }
        val newId = newEntries.getOrNull(newItemPosition)?.let { idExtractor(it) }
        return prevId == newId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return getChangePayload(oldItemPosition, newItemPosition) != null
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val prevEntry = prevEntries.getOrNull(oldItemPosition) ?: return null
        val newEntry = newEntries.getOrNull(newItemPosition) ?: return null
        return payloadCalc?.invoke(prevEntry, newEntry) != null
    }
}
