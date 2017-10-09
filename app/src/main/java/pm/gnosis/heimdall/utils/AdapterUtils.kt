package pm.gnosis.heimdall.utils

import android.support.v7.util.DiffUtil
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.ui.base.Adapter


fun <D> Flowable<List<D>>.scanToAdapterData(itemCheck: ((D, D) -> Boolean), contentCheck: ((D, D) -> Boolean)? = null): Flowable<Adapter.Data<D>> =
        scan(Adapter.Data(), scanner(itemCheck, contentCheck))

fun <D> Observable<List<D>>.scanToAdapterData(itemCheck: ((D, D) -> Boolean), contentCheck: ((D, D) -> Boolean)? = null): Observable<Adapter.Data<D>> =
        scan(Adapter.Data(), scanner(itemCheck, contentCheck))

private fun <D> scanner(itemCheck: (D, D) -> Boolean, contentCheck: ((D, D) -> Boolean)?): (Adapter.Data<D>, List<D>) -> Adapter.Data<D> {
    return { data, newEntries ->
        val diff = DiffUtil.calculateDiff(SimpleDiffCallback(data.entries, newEntries, itemCheck, contentCheck))
        Adapter.Data(data.id, newEntries, diff)
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
