package pm.gnosis.heimdall.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.isAtEnd(threshold: Int = 0): Boolean {
    return (layoutManager as? LinearLayoutManager)?.isAtEnd(threshold) ?: false
}

fun LinearLayoutManager.isAtEnd(threshold: Int = 0): Boolean {
    val itemCount = itemCount
    val visibleItemCount = childCount
    val firstVisiblePosition = findFirstVisibleItemPosition()

    return firstVisiblePosition + visibleItemCount > itemCount - threshold
}
