package io.gnosis.safe

import androidx.paging.*
import kotlinx.coroutines.test.TestCoroutineDispatcher

suspend fun <T : Any> PagingData<T>.collectData(): List<T> {
    val differCallback = object : DifferCallback {
        override fun onChanged(position: Int, count: Int) {}
        override fun onInserted(position: Int, count: Int) {}
        override fun onRemoved(position: Int, count: Int) {}
    }
    val items = mutableListOf<T>()
    val pagingDataDiffer = object : PagingDataDiffer<T>(differCallback, TestCoroutineDispatcher()) {
        override suspend fun presentNewList(
            previousList: NullPaddedList<T>,
            newList: NullPaddedList<T>,
            newCombinedLoadStates: CombinedLoadStates,
            lastAccessedIndex: Int
        ): Int? {
            for (i in 0 until newList.size)
                items.add(newList.getFromStorage(i))
            return null
        }
    }
    pagingDataDiffer.collectFrom(this)
    return items
}
